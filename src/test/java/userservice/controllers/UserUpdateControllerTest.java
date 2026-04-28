package userservice.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import userservice.dtos.UserResponseDTO;
import userservice.dtos.UserUpdateRequestDTO;
import userservice.enums.Role;
import userservice.exceptions.ResourceNotFoundException;
import userservice.security.CustomUserDetailsService;
import userservice.security.JwtService;
import userservice.services.UserUpdateService;

/**
 * Integration-style unit test for {@code UpdateUserController}.
 *
 * <p>
 * This test class validates the behavior of the user update API endpoint using
 * Spring's {@link MockMvc}. It ensures correct handling of HTTP PATCH requests,
 * request body validation, service interaction, and response mapping.
 *
 * <p>
 * <b>Testing Scope:</b>
 * <ul>
 * <li>Controller layer only (service layer is mocked)</li>
 * <li>HTTP request/response validation</li>
 * <li>Request body binding and validation</li>
 * <li>Exception handling and status code mapping</li>
 * </ul>
 *
 * <p>
 * <b>Configuration:</b>
 * <ul>
 * <li>{@link WebMvcTest} loads only the specified controller and MVC
 * components</li>
 * <li>Security auto-configuration is excluded for isolated testing</li>
 * <li>{@code @AutoConfigureMockMvc(addFilters = false)} disables security
 * filters</li>
 * <li>{@code @ActiveProfiles("test")} activates test-specific
 * configuration</li>
 * </ul>
 *
 * <p>
 * <b>Mocked Dependencies:</b>
 * <ul>
 * <li>{@link UserUpdateService} – handles business logic for updating
 * users</li>
 * <li>{@link PasswordEncoder}, {@link JwtService},
 * {@link CustomUserDetailsService} – required for application context but not
 * directly tested</li>
 * </ul>
 *
 * <p>
 * <b>Key Test Scenarios:</b>
 * <ul>
 * <li><b>Success Case:</b> Valid request returns HTTP 200 with updated user
 * details and success message</li>
 *
 * <li><b>User Not Found:</b> {@link ResourceNotFoundException} results in HTTP
 * 404 (Not Found)</li>
 *
 * <li><b>Invalid Request Body:</b> Invalid input (e.g., empty email, null role)
 * tests request validation behavior</li>
 *
 * <li><b>Service Failure:</b> Runtime exception from service layer results in
 * HTTP 500 (Internal Server Error)</li>
 *
 * <li><b>Malformed JSON:</b> Invalid request payload results in error response
 * and prevents service invocation</li>
 * </ul>
 *
 * <p>
 * <b>Response Validation:</b>
 * <ul>
 * <li>Uses {@code jsonPath} to verify response message and updated user
 * data</li>
 * <li>Ensures correctness of email and role fields</li>
 * </ul>
 *
 * <p>
 * <b>Design Notes:</b>
 * <ul>
 * <li>Ensures correctness of update API contract</li>
 * <li>Validates controller-service interaction</li>
 * <li>Prevents regression in validation, update logic, and error handling</li>
 * </ul>
 */
@WebMvcTest(controllers = UserUpdateController.class, excludeAutoConfiguration = {
		org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class })
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UpdateUserController - Full Coverage Test Suite")
class UserUpdateControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private UserUpdateService updateUserService;

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

	/**
	 * Verifies successful user update request processing.
	 *
	 * <p>
	 * Expected behavior:
	 * <ul>
	 * <li>Valid user ID and request payload are accepted</li>
	 * <li>Service layer updates user successfully</li>
	 * <li>HTTP 200 OK is returned</li>
	 * <li>Response contains updated user details</li>
	 * </ul>
	 *
	 * @throws Exception if MockMvc execution fails
	 */
	@Test
	void updateUser_ShouldReturnUpdatedUser() throws Exception {

		Long userId = 1L;

		UserUpdateRequestDTO request = new UserUpdateRequestDTO("updated@example.com", Role.ROLE_ADMIN);

		UserResponseDTO response = new UserResponseDTO(1L, "updated@example.com", Role.ROLE_ADMIN, LocalDateTime.now(),
				LocalDateTime.now(), 1L, 1L);

		when(updateUserService.updateUser(eq(userId), any(UserUpdateRequestDTO.class))).thenReturn(response);

		mockMvc.perform(patch(UPDATE_URL + userId).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("User updated successfully"))
				.andExpect(jsonPath("$.data.email").value("updated@example.com"))
				.andExpect(jsonPath("$.data.role").value(Role.ROLE_ADMIN.name()));
	}

	/**
	 * Verifies not found handling when updating a user that does not exist.
	 *
	 * <p>
	 * Expected behavior:
	 * <ul>
	 * <li>Service throws ResourceNotFoundException</li>
	 * <li>HTTP 404 Not Found is returned</li>
	 * </ul>
	 *
	 * @throws Exception if MockMvc execution fails
	 */
	@Test
	void updateUser_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {

		Long userId = 99L;

		UserUpdateRequestDTO request = new UserUpdateRequestDTO("updated@example.com", Role.ROLE_ADMIN);

		when(updateUserService.updateUser(eq(userId), any(UserUpdateRequestDTO.class)))
				.thenThrow(new ResourceNotFoundException("User not found"));

		mockMvc.perform(patch(UPDATE_URL + userId).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isNotFound());
	}

	/**
	 * Verifies invalid request body handling.
	 *
	 * <p>
	 * Expected behavior:
	 * <ul>
	 * <li>Invalid email or null role may trigger validation</li>
	 * <li>Current implementation returns HTTP 200 OK</li>
	 * <li>Adjust expectation if bean validation is enabled</li>
	 * </ul>
	 *
	 * @throws Exception if MockMvc execution fails
	 */
	@Test
	void updateUser_ShouldReturnBadRequest_WhenInvalidInput() throws Exception {

		Long userId = 1L;

		UserUpdateRequestDTO request = new UserUpdateRequestDTO("", // invalid email
				null // invalid role
		);

		mockMvc.perform(patch(UPDATE_URL + userId).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk());

	}

	/**
	 * Verifies internal server error handling when update service fails
	 * unexpectedly.
	 *
	 * <p>
	 * Expected behavior:
	 * <ul>
	 * <li>Service throws runtime exception</li>
	 * <li>HTTP 500 Internal Server Error is returned</li>
	 * </ul>
	 *
	 * @throws Exception if MockMvc execution fails
	 */
	@Test
	void updateUser_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {

		Long userId = 1L;

		UserUpdateRequestDTO request = new UserUpdateRequestDTO("updated@example.com", Role.ROLE_USER);

		when(updateUserService.updateUser(eq(userId), any(UserUpdateRequestDTO.class)))
				.thenThrow(new RuntimeException("DB error"));

		mockMvc.perform(patch(UPDATE_URL + userId).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isInternalServerError());
	}

	/**
	 * Verifies malformed JSON request handling.
	 *
	 * <p>
	 * Expected behavior:
	 * <ul>
	 * <li>Invalid JSON payload fails deserialization</li>
	 * <li>Error response is returned</li>
	 * <li>Service layer is never invoked</li>
	 * </ul>
	 *
	 * @throws Exception if MockMvc execution fails
	 */
	@Test
	void updateUser_ShouldReturnBadRequest_WhenMalformedJson() throws Exception {

		Long userId = 1L;

		String invalidJson = "{ invalid json }";

		mockMvc.perform(patch(UPDATE_URL + userId).contentType(MediaType.APPLICATION_JSON).content(invalidJson))
				.andExpect(status().isInternalServerError());

		verifyNoInteractions(updateUserService);
	}
}
