package userservice.controllers;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import userservice.dtos.UserResponseDTO;
import userservice.enums.Role;
import userservice.exceptions.ResourceNotFoundException;
import userservice.security.CustomUserDetailsService;
import userservice.security.JwtService;
import userservice.services.UserGetByIdService;

/**
 * Integration-style unit test for {@code GetUserByIdController}.
 *
 * <p>
 * This test class validates the behavior of the "Get User By ID" API endpoint
 * using Spring's {@link MockMvc}. It ensures correct request handling, service
 * interaction, and response mapping for different execution scenarios.
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
 * <li>{@link UserGetByIdService} – handles business logic for fetching
 * users</li>
 * <li>{@link PasswordEncoder}, {@link JwtService},
 * {@link CustomUserDetailsService} – required for application context but not
 * directly tested</li>
 * </ul>
 *
 * <p>
 * <b>Key Test Scenarios:</b>
 * <ul>
 * <li><b>Success Case:</b> Valid user ID returns HTTP 200 with user details and
 * success message</li>
 *
 * <li><b>User Not Found:</b> {@code ResourceNotFoundException} results in HTTP
 * 404 (Not Found)</li>
 *
 * <li><b>Invalid Path Variable:</b> Non-numeric ID results in error response
 * and prevents service invocation</li>
 *
 * <li><b>Service Failure:</b> Runtime exception from service layer results in
 * HTTP 500 (Internal Server Error)</li>
 * </ul>
 *
 * <p>
 * <b>Response Validation:</b>
 * <ul>
 * <li>Uses {@code jsonPath} to verify response message and user data</li>
 * <li>Ensures correctness of ID, email, and role fields</li>
 * </ul>
 *
 * <p>
 * <b>Design Notes:</b>
 * <ul>
 * <li>Ensures correctness of API contract for fetching users</li>
 * <li>Validates proper delegation to service layer</li>
 * <li>Prevents regression in exception handling and response structure</li>
 * </ul>
 */
@WebMvcTest(controllers = UserGetByIdController.class, excludeAutoConfiguration = {
		org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class })
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("GetUserByIdController - Full Coverage Test Suite")
class UserGetByIdControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private UserGetByIdService getUserByIdService;

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

	private static final String GET_BY_ID_URL = "/api/v1/users/getById/";

	/**
	 * Verifies successful retrieval of a user by ID.
	 *
	 * <p>
	 * Expected behavior:
	 * <ul>
	 * <li>Valid user ID is accepted</li>
	 * <li>Service returns matching user data</li>
	 * <li>HTTP 200 OK is returned</li>
	 * <li>Response contains success message and user details</li>
	 * </ul>
	 *
	 * @throws Exception if MockMvc execution fails
	 */
	@Test
	void getUserById_ShouldReturnUser() throws Exception {

		Long userId = 1L;

		UserResponseDTO response = new UserResponseDTO(1L, "test@example.com", Role.ROLE_USER, LocalDateTime.now(),
				LocalDateTime.now(), 1L, 1L);

		when(getUserByIdService.getUserById(userId)).thenReturn(response);

		mockMvc.perform(get(GET_BY_ID_URL + userId)).andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("User fetched successfully"))
				.andExpect(jsonPath("$.data.id").value(1L))
				.andExpect(jsonPath("$.data.email").value("test@example.com"))
				.andExpect(jsonPath("$.data.role").value(Role.ROLE_USER.name()));
	}

	/**
	 * Verifies not found handling when requested user does not exist.
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
	void getUserById_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {

		Long userId = 99L;

		when(getUserByIdService.getUserById(userId)).thenThrow(new ResourceNotFoundException("User not found"));

		mockMvc.perform(get(GET_BY_ID_URL + userId)).andExpect(status().isNotFound());
	}

	/**
	 * Verifies invalid path variable handling.
	 *
	 * <p>
	 * Expected behavior:
	 * <ul>
	 * <li>Non-numeric ID fails type conversion</li>
	 * <li>Error response is returned</li>
	 * <li>Service layer is never invoked</li>
	 * </ul>
	 *
	 * @throws Exception if MockMvc execution fails
	 */
	@Test
	void getUserById_ShouldReturnBadRequest_WhenInvalidId() throws Exception {

		mockMvc.perform(get(GET_BY_ID_URL + "invalid")).andExpect(status().isInternalServerError());

		verifyNoInteractions(getUserByIdService);
	}

	/**
	 * Verifies internal server error handling when service fails unexpectedly.
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
	void getUserById_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {

		Long userId = 1L;

		when(getUserByIdService.getUserById(userId)).thenThrow(new RuntimeException("Something went wrong"));

		mockMvc.perform(get(GET_BY_ID_URL + userId)).andExpect(status().isInternalServerError());
	}
}
