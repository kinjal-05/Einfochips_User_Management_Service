package userservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import userservice.dtos.ChangePasswordRequestDTO;
import userservice.exceptions.ResourceNotFoundException;
import userservice.security.CustomUserDetailsService;
import userservice.security.JwtService;
import userservice.services.ChangePasswordService;
import userservice.services.UpdateUserService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
		controllers = ChangePasswordController.class,
		excludeAutoConfiguration = {
				org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
		}
)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ChangePasswordController - Full Coverage Test Suite")
public class ChangePasswordControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ChangePasswordService changePasswordService;

	@MockBean
	private PasswordEncoder passwordEncoder;

	@MockBean
	private JwtService jwtService;

	@MockBean
	private CustomUserDetailsService customUserDetailsService;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void setup() {
		objectMapper.registerModule(new JavaTimeModule());
	}

	private static final String CHANGE_PASSWORD_URL = "/api/v1/users/changePassword";

	// 1. SUCCESS
	@Test
	void changePassword_ShouldReturnSuccess() throws Exception {

		ChangePasswordRequestDTO request = new ChangePasswordRequestDTO(
				"oldPassword@123",
				"newPassword@123"
		);

		doNothing().when(changePasswordService).changePassword(any(ChangePasswordRequestDTO.class));

		mockMvc.perform(patch(CHANGE_PASSWORD_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Password changed successfully"));

		verify(changePasswordService).changePassword(any(ChangePasswordRequestDTO.class));
	}

	// 2. VALIDATION FAILURE
	@Test
	void changePassword_ShouldReturnBadRequest_WhenValidationFails() throws Exception {

		ChangePasswordRequestDTO request = new ChangePasswordRequestDTO(
				"",   // invalid old password
				""    // invalid new password
		);

		mockMvc.perform(patch(CHANGE_PASSWORD_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(changePasswordService);
	}

	//  3. BAD CREDENTIALS → 401
	@Test
	void changePassword_ShouldReturnUnauthorized_WhenWrongPassword() throws Exception {

		ChangePasswordRequestDTO request = new ChangePasswordRequestDTO(
				"wrongOldPassword",
				"newPassword@123"
		);

		doThrow(new org.springframework.security.authentication.BadCredentialsException("Invalid password"))
				.when(changePasswordService).changePassword(any(ChangePasswordRequestDTO.class));

		mockMvc.perform(patch(CHANGE_PASSWORD_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnauthorized());
	}

	// 4. USER NOT FOUND → 404
	@Test
	void changePassword_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {

		ChangePasswordRequestDTO request = new ChangePasswordRequestDTO(
				"oldPassword@123",
				"newPassword@123"
		);

		doThrow(new ResourceNotFoundException("User not found"))
				.when(changePasswordService).changePassword(any(ChangePasswordRequestDTO.class));

		mockMvc.perform(patch(CHANGE_PASSWORD_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound());
	}

	//  5. SERVICE FAILURE → 500
	@Test
	void changePassword_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {

		ChangePasswordRequestDTO request = new ChangePasswordRequestDTO(
				"oldPassword@123",
				"newPassword@123"
		);

		doThrow(new RuntimeException("DB error"))
				.when(changePasswordService).changePassword(any(ChangePasswordRequestDTO.class));

		mockMvc.perform(patch(CHANGE_PASSWORD_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isInternalServerError());
	}

	//  6. MALFORMED JSON
	@Test
	void changePassword_ShouldReturnBadRequest_WhenMalformedJson() throws Exception {

		String invalidJson = "{ invalid json }";

		mockMvc.perform(patch(CHANGE_PASSWORD_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalidJson))
				.andExpect(status().isInternalServerError());

		verifyNoInteractions(changePasswordService);
	}
}
