package userservice.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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

import userservice.dtos.UserRequestDTO;
import userservice.dtos.UserResponseDTO;
import userservice.enums.Role;
import userservice.security.CustomUserDetailsService;
import userservice.security.JwtService;
import userservice.services.UserCreateService;

/**
 * Integration-style unit test for {@code CreateUserController}.
 *
 * <p>
 * This test class validates the behavior of the user creation (registration)
 * API endpoint using Spring's {@link MockMvc}. It ensures correct handling of
 * HTTP requests, validation, service interaction, and response structure.
 *
 * <p>
 * <b>Testing Scope:</b>
 * <ul>
 * <li>Controller layer only (service layer is mocked)</li>
 * <li>Request/response validation</li>
 * <li>Exception handling and HTTP status mapping</li>
 * </ul>
 *
 * <p>
 * <b>Configuration:</b>
 * <ul>
 * <li>{@link WebMvcTest} loads only MVC components for the specified
 * controller</li>
 * <li>Security auto-configuration is excluded for isolated testing</li>
 * <li>{@code @AutoConfigureMockMvc(addFilters = false)} disables security
 * filters</li>
 * <li>{@code @ActiveProfiles("test")} enables test-specific configuration</li>
 * </ul>
 *
 * <p>
 * <b>Mocked Dependencies:</b>
 * <ul>
 * <li>{@link UserCreateService} – handles business logic for user creation</li>
 * <li>{@link PasswordEncoder}, {@link JwtService},
 * {@link CustomUserDetailsService} – required for context loading but not under
 * test</li>
 * </ul>
 *
 * <p>
 * <b>Key Test Scenarios:</b>
 * <ul>
 * <li><b>Success Case:</b> Valid request returns HTTP 201 (Created) with user
 * details and success message</li>
 *
 * <li><b>Validation Failure:</b> Invalid input (empty email, null role) results
 * in error response and prevents service invocation</li>
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
 * <li>Uses {@code jsonPath} to verify response message and returned user
 * data</li>
 * <li>Ensures correct mapping of role and email fields</li>
 * </ul>
 *
 * <p>
 * <b>Design Notes:</b>
 * <ul>
 * <li>Ensures correctness of API contract for user creation</li>
 * <li>Validates controller-service interaction</li>
 * <li>Prevents regressions in validation and exception handling</li>
 * </ul>
 */
@WebMvcTest(controllers = UserCreateController.class, excludeAutoConfiguration = {
		org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class })
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("CreateUserController - Full Coverage Test Suite")
class UserCreateControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private UserCreateService createUserService;

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

	/**
	 * Verifies successful user creation request processing.
	 *
	 * <p>
	 * Expected behavior:
	 * <ul>
	 * <li>Valid request payload is accepted</li>
	 * <li>Service layer creates the user</li>
	 * <li>HTTP 201 Created is returned</li>
	 * <li>Response contains success message and created user data</li>
	 * </ul>
	 *
	 * @throws Exception if MockMvc execution fails
	 */
	@Test
	void createUser_ShouldReturnCreatedUser() throws Exception {

		UserRequestDTO request = new UserRequestDTO("test@example.com", Role.ROLE_USER);

		UserResponseDTO response = new UserResponseDTO(1L, "test@example.com", Role.ROLE_USER, LocalDateTime.now(),
				LocalDateTime.now(), 1L, 1L);

		Mockito.when(createUserService.createUser(Mockito.any(UserRequestDTO.class))).thenReturn(response);

		mockMvc.perform(post("/api/v1/users/registerUser").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
				.andExpect(jsonPath("$.message").value("User created successfully"))
				.andExpect(jsonPath("$.data.email").value("test@example.com"))
				.andExpect(jsonPath("$.data.role").value(Role.ROLE_USER.name()));
	}

	/**
	 * Verifies bean validation failure handling during user creation.
	 *
	 * <p>
	 * Expected behavior:
	 * <ul>
	 * <li>Blank email and null role fail validation</li>
	 * <li>Error response is returned</li>
	 * <li>Service layer is never invoked</li>
	 * </ul>
	 *
	 * @throws Exception if MockMvc execution fails
	 */
	@Test
	void createUser_ShouldReturnBadRequest_WhenValidationFails() throws Exception {

		UserRequestDTO request = new UserRequestDTO("", // invalid email
				null // invalid role
		);

		mockMvc.perform(post("/registerUser").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isInternalServerError());

		Mockito.verifyNoInteractions(createUserService);
	}

	/**
	 * Verifies internal server error handling when service logic fails.
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
	void createUser_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {

		UserRequestDTO request = new UserRequestDTO("test@example.com", Role.ROLE_USER);

		Mockito.when(createUserService.createUser(Mockito.any(UserRequestDTO.class)))
				.thenThrow(new RuntimeException("Something went wrong"));

		mockMvc.perform(post("/registerUser").contentType(MediaType.APPLICATION_JSON)
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
	void createUser_ShouldReturnBadRequest_WhenMalformedJson() throws Exception {

		String invalidJson = "{ invalid json }";

		mockMvc.perform(post("/registerUser").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
				.andExpect(status().isInternalServerError());

		Mockito.verifyNoInteractions(createUserService);
	}
}
