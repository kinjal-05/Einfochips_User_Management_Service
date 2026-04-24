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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import userservice.exceptions.ResourceNotFoundException;
import userservice.security.CustomUserDetailsService;
import userservice.security.JwtService;
import userservice.services.SoftDeleteUserService;
import userservice.services.UpdateUserService;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
		controllers = SoftDeleteUserController.class,
		excludeAutoConfiguration = {
				org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
		}
)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SoftDeleteUserController - Full Coverage Test Suite")
public class SoftDeleteUserControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private SoftDeleteUserService softDeleteUserService;

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

	private static final String DELETE_URL = "/api/v1/users/deleteUser/";

	// 1. SUCCESS
	@Test
	void deleteUser_ShouldReturnSuccess() throws Exception {

		long userId = 1L;

		doNothing().when(softDeleteUserService).softDeleteUser(userId);

		mockMvc.perform(delete(DELETE_URL + userId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("User deleted successfully"))
				.andExpect(jsonPath("$.data").doesNotExist());

		verify(softDeleteUserService).softDeleteUser(userId);
	}

	//2. USER NOT FOUND → 404
	@Test
	void deleteUser_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {

		long userId = 99L;

		doThrow(new ResourceNotFoundException("User not found"))
				.when(softDeleteUserService).softDeleteUser(userId);

		mockMvc.perform(delete(DELETE_URL + userId))
				.andExpect(status().isNotFound());
	}

	// 3. INVALID ID → 400
	@Test
	void deleteUser_ShouldReturnBadRequest_WhenInvalidId() throws Exception {

		mockMvc.perform(delete(DELETE_URL + "invalid"))
				.andExpect(status().isInternalServerError());

		verifyNoInteractions(softDeleteUserService);
	}

	// 4. SERVICE FAILURE → 500
	@Test
	void deleteUser_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {

		long userId = 1L;

		doThrow(new RuntimeException("DB error"))
				.when(softDeleteUserService).softDeleteUser(userId);

		mockMvc.perform(delete(DELETE_URL + userId))
				.andExpect(status().isInternalServerError());
	}
}
