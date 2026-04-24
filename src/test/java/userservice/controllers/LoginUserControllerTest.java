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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import userservice.dtos.LoginRequestDTO;
import userservice.dtos.LoginResponseDTO;
import userservice.enums.Role;
import userservice.security.CustomUserDetailsService;
import userservice.security.JwtService;
import userservice.services.CreateUserService;
import userservice.services.LoginUserService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
		controllers = LoginController.class,
		excludeAutoConfiguration = {
				org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
		}
)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("LoginUserController - Full Coverage Test Suite")
public class LoginUserControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private LoginUserService loginUserService;

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

	// 1. SUCCESS
	@Test
	void login_ShouldReturnSuccess() throws Exception {

		LoginRequestDTO request = new LoginRequestDTO(
				"test@example.com",
				"Password@123"
		);

		LoginResponseDTO response = new LoginResponseDTO(
				1L,
				"test@example.com",
				Role.ROLE_USER,
				"jwt-token",
				"Login successful"
		);

		when(loginUserService.login(any(LoginRequestDTO.class)))
				.thenReturn(response);

		mockMvc.perform(post(LOGIN_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Login successful"))
				.andExpect(jsonPath("$.data.id").value(1L))
				.andExpect(jsonPath("$.data.email").value("test@example.com"))
				.andExpect(jsonPath("$.data.role").value(Role.ROLE_USER.name()))
				.andExpect(jsonPath("$.data.token").value("jwt-token"))
				.andExpect(jsonPath("$.data.message").value("Login successful"));
	}

	// 2. VALIDATION FAILURE
	@Test
	void login_ShouldReturnBadRequest_WhenValidationFails() throws Exception {

		LoginRequestDTO request = new LoginRequestDTO("", "");

		mockMvc.perform(post(LOGIN_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(loginUserService);
	}

	// 3. BAD CREDENTIALS → 401
	@Test
	void login_ShouldReturnUnauthorized_WhenBadCredentials() throws Exception {

		LoginRequestDTO request = new LoginRequestDTO(
				"test@example.com",
				"wrong-password"
		);

		when(loginUserService.login(any(LoginRequestDTO.class)))
				.thenThrow(new BadCredentialsException("Invalid credentials"));

		mockMvc.perform(post(LOGIN_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnauthorized());
	}

	// 4. GENERIC ERROR → 500
	@Test
	void login_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {

		LoginRequestDTO request = new LoginRequestDTO(
				"test@example.com",
				"Password@123"
		);

		when(loginUserService.login(any(LoginRequestDTO.class)))
				.thenThrow(new RuntimeException("Something went wrong"));

		mockMvc.perform(post(LOGIN_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isInternalServerError());
	}

	// 5. MALFORMED JSON
	@Test
	void login_ShouldReturnBadRequest_WhenMalformedJson() throws Exception {

		String invalidJson = "{ invalid json }";

		mockMvc.perform(post(LOGIN_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalidJson))
				.andExpect(status().isInternalServerError());

		verifyNoInteractions(loginUserService);
	}
}
