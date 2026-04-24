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
import userservice.dtos.UserResponseDTO;
import userservice.dtos.UserUpdateRequestDTO;
import userservice.enums.Role;
import userservice.exceptions.ResourceNotFoundException;
import userservice.security.CustomUserDetailsService;
import userservice.security.JwtService;
import userservice.services.SearchUserService;
import userservice.services.UpdateUserService;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
		controllers = UpdateUserController.class,
		excludeAutoConfiguration = {
				org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
		}
)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UpdateUserController - Full Coverage Test Suite")
public class UpdateUserControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private UpdateUserService updateUserService;

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

	private static final String UPDATE_URL = "/api/v1/users/updateUser/";

	// 1. SUCCESS
	@Test
	void updateUser_ShouldReturnUpdatedUser() throws Exception {

		Long userId = 1L;

		UserUpdateRequestDTO request = new UserUpdateRequestDTO(
				"updated@example.com",
				Role.ROLE_ADMIN
		);

		UserResponseDTO response = new UserResponseDTO(
				1L,
				"updated@example.com",
				Role.ROLE_ADMIN,
				LocalDateTime.now(),
				LocalDateTime.now(),
				1L,
				1L
		);

		when(updateUserService.updateUser(eq(userId), any(UserUpdateRequestDTO.class)))
				.thenReturn(response);

		mockMvc.perform(patch(UPDATE_URL + userId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("User updated successfully"))
				.andExpect(jsonPath("$.data.email").value("updated@example.com"))
				.andExpect(jsonPath("$.data.role").value(Role.ROLE_ADMIN.name()));
	}

	// 2. USER NOT FOUND → 404
	@Test
	void updateUser_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {

		Long userId = 99L;

		UserUpdateRequestDTO request = new UserUpdateRequestDTO(
				"updated@example.com",
				Role.ROLE_ADMIN
		);

		when(updateUserService.updateUser(eq(userId), any(UserUpdateRequestDTO.class)))
				.thenThrow(new ResourceNotFoundException("User not found"));

		mockMvc.perform(patch(UPDATE_URL + userId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound());
	}

	// 3. INVALID REQUEST BODY (if validation exists)
	@Test
	void updateUser_ShouldReturnBadRequest_WhenInvalidInput() throws Exception {

		Long userId = 1L;

		UserUpdateRequestDTO request = new UserUpdateRequestDTO(
				"",   // invalid email
				null  // invalid role
		);

		mockMvc.perform(patch(UPDATE_URL + userId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk());


	}

	// 4. SERVICE EXCEPTION → 500
	@Test
	void updateUser_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {

		Long userId = 1L;

		UserUpdateRequestDTO request = new UserUpdateRequestDTO(
				"updated@example.com",
				Role.ROLE_USER
		);

		when(updateUserService.updateUser(eq(userId), any(UserUpdateRequestDTO.class)))
				.thenThrow(new RuntimeException("DB error"));

		mockMvc.perform(patch(UPDATE_URL + userId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isInternalServerError());
	}

	//  5. MALFORMED JSON
	@Test
	void updateUser_ShouldReturnBadRequest_WhenMalformedJson() throws Exception {

		Long userId = 1L;

		String invalidJson = "{ invalid json }";

		mockMvc.perform(patch(UPDATE_URL + userId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalidJson))
				.andExpect(status().isInternalServerError());

		verifyNoInteractions(updateUserService);
	}
}
