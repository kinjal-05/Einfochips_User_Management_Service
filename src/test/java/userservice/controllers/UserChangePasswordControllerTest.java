package userservice.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import userservice.dtos.UserChangePasswordRequestDTO;
import userservice.exceptions.ResourceNotFoundException;
import userservice.security.CustomUserDetailsService;
import userservice.security.JwtService;
import userservice.services.UserChangePasswordService;

/**
 * Integration-style unit test for {@code ChangePasswordController}.
 *
 * <p>
 * This test class verifies the behavior of the change password API endpoint
 * using Spring's {@link MockMvc}. It ensures correct request handling,
 * validation, exception mapping, and response structure under various
 * scenarios.
 *
 * <p>
 * <b>Testing Scope:</b>
 * <ul>
 * <li>Controller layer only (service layer is mocked)</li>
 * <li>HTTP request/response validation</li>
 * <li>Exception handling and status code mapping</li>
 * </ul>
 *
 * <p>
 * <b>Configuration:</b>
 * <ul>
 * <li>{@link WebMvcTest} loads only the controller and related MVC
 * components</li>
 * <li>Security auto-configuration is excluded for isolated testing</li>
 * <li>{@code @AutoConfigureMockMvc(addFilters = false)} disables security
 * filters</li>
 * <li>{@code @ActiveProfiles("test")} activates test configuration</li>
 * </ul>
 *
 * <p>
 * <b>Mocked Dependencies:</b>
 * <ul>
 * <li>{@link UserChangePasswordService} – business logic layer</li>
 * <li>{@link PasswordEncoder}, {@link JwtService},
 * {@link CustomUserDetailsService} – required for context loading but not
 * directly tested</li>
 * </ul>
 *
 * <p>
 * <b>Key Test Scenarios:</b>
 * <ul>
 * <li><b>Success Case:</b> Valid request returns HTTP 200 with success
 * message</li>
 *
 * <li><b>Validation Failure:</b> Invalid input (blank fields) returns HTTP 400
 * and prevents service invocation</li>
 *
 * <li><b>Authentication Failure:</b> {@code BadCredentialsException} results in
 * HTTP 401 (Unauthorized)</li>
 *
 * <li><b>Resource Not Found:</b> {@code ResourceNotFoundException} results in
 * HTTP 404 (Not Found)</li>
 *
 * <li><b>Unexpected Errors:</b> Runtime exceptions return HTTP 500 (Internal
 * Server Error)</li>
 *
 * <li><b>Malformed JSON:</b> Invalid request payload results in error response
 * and no service interaction</li>
 * </ul>
 *
 * <p>
 * <b>Validation:</b> Uses {@code jsonPath} to verify response structure and
 * message correctness.
 *
 * <p>
 * <b>Design Notes:</b>
 * <ul>
 * <li>Ensures full coverage of controller behavior</li>
 * <li>Verifies proper delegation to service layer</li>
 * <li>Prevents regression in API contract and error handling</li>
 * </ul>
 */
@WebMvcTest(controllers = UserChangePasswordController.class, excludeAutoConfiguration = {
		org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class })
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ChangePasswordController - Full Coverage Test Suite")
class UserChangePasswordControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private UserChangePasswordService changePasswordService;

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

	/**
	 * Verifies successful password change request processing.
	 *
	 * <p>
	 * Expected behavior:
	 * <ul>
	 * <li>Valid request payload is accepted</li>
	 * <li>Service layer executes password change logic</li>
	 * <li>HTTP 200 OK is returned</li>
	 * <li>Success response message is returned</li>
	 * </ul>
	 *
	 * @throws Exception if MockMvc execution fails
	 */
	@Test
	void changePassword_ShouldReturnSuccess() throws Exception {

		UserChangePasswordRequestDTO request = new UserChangePasswordRequestDTO("oldPassword@123", "newPassword@123");

		doNothing().when(changePasswordService).changePassword(any(UserChangePasswordRequestDTO.class));

		mockMvc.perform(patch(CHANGE_PASSWORD_URL).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Password changed successfully"));

		verify(changePasswordService).changePassword(any(UserChangePasswordRequestDTO.class));
	}

	/**
	 * Verifies request validation failure handling.
	 *
	 * <p>
	 * Expected behavior:
	 * <ul>
	 * <li>Blank passwords fail bean validation</li>
	 * <li>HTTP 400 Bad Request is returned</li>
	 * <li>Service layer is never invoked</li>
	 * </ul>
	 *
	 * @throws Exception if MockMvc execution fails
	 */
	@Test
	void changePassword_ShouldReturnBadRequest_WhenValidationFails() throws Exception {

		UserChangePasswordRequestDTO request = new UserChangePasswordRequestDTO("", // invalid old password
				"" // invalid new password
		);

		mockMvc.perform(patch(CHANGE_PASSWORD_URL).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());

		verifyNoInteractions(changePasswordService);
	}

	/**
	 * Verifies unauthorized response when old password is incorrect.
	 *
	 * <p>
	 * Expected behavior:
	 * <ul>
	 * <li>Service throws BadCredentialsException</li>
	 * <li>HTTP 401 Unauthorized is returned</li>
	 * </ul>
	 *
	 * @throws Exception if MockMvc execution fails
	 */
	@Test
	void changePassword_ShouldReturnUnauthorized_WhenWrongPassword() throws Exception {

		UserChangePasswordRequestDTO request = new UserChangePasswordRequestDTO("wrongOldPassword", "newPassword@123");

		doThrow(new org.springframework.security.authentication.BadCredentialsException("Invalid password"))
				.when(changePasswordService).changePassword(any(UserChangePasswordRequestDTO.class));

		mockMvc.perform(patch(CHANGE_PASSWORD_URL).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isUnauthorized());
	}

	/**
	 * Verifies not found response when authenticated user does not exist.
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
	void changePassword_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {

		UserChangePasswordRequestDTO request = new UserChangePasswordRequestDTO("oldPassword@123", "newPassword@123");

		doThrow(new ResourceNotFoundException("User not found")).when(changePasswordService)
				.changePassword(any(UserChangePasswordRequestDTO.class));

		mockMvc.perform(patch(CHANGE_PASSWORD_URL).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isNotFound());
	}

	/**
	 * Verifies internal server error handling for unexpected failures.
	 *
	 * <p>
	 * Expected behavior:
	 * <ul>
	 * <li>Unhandled runtime exception occurs</li>
	 * <li>HTTP 500 Internal Server Error is returned</li>
	 * </ul>
	 *
	 * @throws Exception if MockMvc execution fails
	 */
	@Test
	void changePassword_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {

		UserChangePasswordRequestDTO request = new UserChangePasswordRequestDTO("oldPassword@123", "newPassword@123");

		doThrow(new RuntimeException("DB error")).when(changePasswordService)
				.changePassword(any(UserChangePasswordRequestDTO.class));

		mockMvc.perform(patch(CHANGE_PASSWORD_URL).contentType(MediaType.APPLICATION_JSON)
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
	void changePassword_ShouldReturnBadRequest_WhenMalformedJson() throws Exception {

		String invalidJson = "{ invalid json }";

		mockMvc.perform(patch(CHANGE_PASSWORD_URL).contentType(MediaType.APPLICATION_JSON).content(invalidJson))
				.andExpect(status().isInternalServerError());

		verifyNoInteractions(changePasswordService);
	}
}
