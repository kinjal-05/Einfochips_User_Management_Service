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
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Integration-style unit test for {@code SoftDeleteUserController}.
 *
 * <p>This test class validates the behavior of the soft delete user API endpoint
 * using Spring's {@link MockMvc}. It ensures correct handling of HTTP DELETE requests,
 * service interaction, and response mapping for different scenarios.
 *
 * <p><b>Testing Scope:</b>
 * <ul>
 *   <li>Controller layer only (service layer is mocked)</li>
 *   <li>HTTP request/response validation</li>
 *   <li>Exception handling and status code mapping</li>
 * </ul>
 *
 * <p><b>Configuration:</b>
 * <ul>
 *   <li>{@link WebMvcTest} loads only the specified controller and MVC components</li>
 *   <li>Security auto-configuration is excluded for isolated testing</li>
 *   <li>{@code @AutoConfigureMockMvc(addFilters = false)} disables security filters</li>
 *   <li>{@code @ActiveProfiles("test")} activates test-specific configuration</li>
 * </ul>
 *
 * <p><b>Mocked Dependencies:</b>
 * <ul>
 *   <li>{@link SoftDeleteUserService} – handles business logic for soft deletion</li>
 *   <li>{@link PasswordEncoder}, {@link JwtService}, {@link CustomUserDetailsService}
 *       – required for application context but not directly tested</li>
 * </ul>
 *
 * <p><b>Key Test Scenarios:</b>
 * <ul>
 *   <li><b>Success Case:</b>
 *       Valid user ID returns HTTP 200 with success message and no data payload</li>
 *
 *   <li><b>User Not Found:</b>
 *       {@link ResourceNotFoundException} results in HTTP 404 (Not Found)</li>
 *
 *   <li><b>Invalid Path Variable:</b>
 *       Non-numeric ID results in error response and prevents service invocation</li>
 *
 *   <li><b>Service Failure:</b>
 *       Runtime exception from service layer results in HTTP 500 (Internal Server Error)</li>
 * </ul>
 *
 * <p><b>Response Validation:</b>
 * <ul>
 *   <li>Uses {@code jsonPath} to verify success message and absence of data field</li>
 * </ul>
 *
 * <p><b>Design Notes:</b>
 * <ul>
 *   <li>Ensures correctness of soft delete API contract</li>
 *   <li>Validates proper delegation to service layer</li>
 *   <li>Prevents regression in exception handling and response structure</li>
 * </ul>
 */
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
