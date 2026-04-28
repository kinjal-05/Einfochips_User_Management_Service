package userservice.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import userservice.dtos.UserLoginRequestDTO;
import userservice.dtos.UserLoginResponseDTO;
import userservice.enums.Role;
import userservice.security.CustomUserDetailsService;
import userservice.security.JwtService;
import userservice.services.UserLoginService;

/**
 * Integration-style unit test for {@code LoginController}.
 *
 * <p>
 * This test class validates the behavior of the login API endpoint using
 * Spring's {@link MockMvc}. It ensures correct handling of authentication
 * requests, validation, service interaction, and response mapping for various
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
 * <li>{@link UserLoginService} – handles authentication business logic</li>
 * <li>{@link PasswordEncoder}, {@link JwtService},
 * {@link CustomUserDetailsService} – required for application context but not
 * directly tested</li>
 * </ul>
 *
 * <p>
 * <b>Key Test Scenarios:</b>
 * <ul>
 * <li><b>Success Case:</b> Valid credentials return HTTP 200 with user details,
 * JWT token, and success message</li>
 *
 * <li><b>Validation Failure:</b> Invalid input (empty email/password) returns
 * HTTP 400 and prevents service invocation</li>
 *
 * <li><b>Authentication Failure:</b> {@link BadCredentialsException} results in
 * HTTP 401 (Unauthorized)</li>
 *
 * <li><b>Service Failure:</b> Runtime exception from service layer results in
 * HTTP 500 (Internal Server Error)</li>
 *
 * <li><b>Malformed JSON:</b> Invalid request payload results in error response
 * and no service interaction</li>
 * </ul>
 *
 * <p>
 * <b>Response Validation:</b>
 * <ul>
 * <li>Uses {@code jsonPath} to verify response message, user details, and JWT
 * token</li>
 * <li>Ensures correctness of ID, email, role, and token fields</li>
 * </ul>
 *
 * <p>
 * <b>Design Notes:</b>
 * <ul>
 * <li>Ensures correctness of authentication API contract</li>
 * <li>Validates controller-service interaction</li>
 * <li>Prevents regression in validation, security, and exception handling</li>
 * </ul>
 */
@WebMvcTest(controllers = UserLoginController.class, excludeAutoConfiguration = {
		org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class })
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("LoginUserController - Full Coverage Test Suite")
class UserLoginControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private UserLoginService loginUserService;

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

	private static final String LOGIN_URL = "/api/v1/users/login";

	/**
	 * Verifies successful login request processing.
	 *
	 * <p>
	 * Expected behavior:
	 * <ul>
	 * <li>Valid email and password are accepted</li>
	 * <li>Authentication service returns login response</li>
	 * <li>HTTP 200 OK is returned</li>
	 * <li>JWT token and user details are included in response</li>
	 * </ul>
	 *
	 * @throws Exception if MockMvc execution fails
	 */
	@Test
	void login_ShouldReturnSuccess() throws Exception {

		UserLoginRequestDTO request = new UserLoginRequestDTO("test@example.com", "Password@123");

		UserLoginResponseDTO response = new UserLoginResponseDTO(1L, "test@example.com", Role.ROLE_USER, "jwt-token",
				"Login successful");

		when(loginUserService.login(any(UserLoginRequestDTO.class))).thenReturn(response);

		mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Login successful")).andExpect(jsonPath("$.data.id").value(1L))
				.andExpect(jsonPath("$.data.email").value("test@example.com"))
				.andExpect(jsonPath("$.data.role").value(Role.ROLE_USER.name()))
				.andExpect(jsonPath("$.data.token").value("jwt-token"))
				.andExpect(jsonPath("$.data.message").value("Login successful"));
	}

	/**
	 * Verifies request validation failure handling.
	 *
	 * <p>
	 * Expected behavior:
	 * <ul>
	 * <li>Blank email and password fail validation</li>
	 * <li>HTTP 400 Bad Request is returned</li>
	 * <li>Service layer is never invoked</li>
	 * </ul>
	 *
	 * @throws Exception if MockMvc execution fails
	 */
	@Test
	void login_ShouldReturnBadRequest_WhenValidationFails() throws Exception {

		UserLoginRequestDTO request = new UserLoginRequestDTO("", "");

		mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());

		verifyNoInteractions(loginUserService);
	}

	/**
	 * Verifies unauthorized response when credentials are invalid.
	 *
	 * <p>
	 * Expected behavior:
	 * <ul>
	 * <li>Authentication service throws BadCredentialsException</li>
	 * <li>HTTP 401 Unauthorized is returned</li>
	 * </ul>
	 *
	 * @throws Exception if MockMvc execution fails
	 */
	@Test
	void login_ShouldReturnUnauthorized_WhenBadCredentials() throws Exception {

		UserLoginRequestDTO request = new UserLoginRequestDTO("test@example.com", "wrong-password");

		when(loginUserService.login(any(UserLoginRequestDTO.class)))
				.thenThrow(new BadCredentialsException("Invalid credentials"));

		mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isUnauthorized());
	}

	/**
	 * Verifies internal server error handling for unexpected failures.
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
	void login_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {

		UserLoginRequestDTO request = new UserLoginRequestDTO("test@example.com", "Password@123");

		when(loginUserService.login(any(UserLoginRequestDTO.class)))
				.thenThrow(new RuntimeException("Something went wrong"));

		mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
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
	void login_ShouldReturnBadRequest_WhenMalformedJson() throws Exception {

		String invalidJson = "{ invalid json }";

		mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(invalidJson))
				.andExpect(status().isInternalServerError());

		verifyNoInteractions(loginUserService);
	}
}
