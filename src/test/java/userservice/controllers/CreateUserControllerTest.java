package userservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import userservice.dtos.UserRequestDTO;
import userservice.dtos.UserResponseDTO;
import userservice.enums.Role;
import userservice.security.CustomUserDetailsService;
import userservice.security.JwtService;
import userservice.services.CreateUserService;
import userservice.services.UserService;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
		controllers = CreateUserController.class,
		excludeAutoConfiguration = {
				org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
		}
)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("CreateUserController - Full Coverage Test Suite")
public class CreateUserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private CreateUserService createUserService;

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

	@Test
	void createUser_ShouldReturnCreatedUser() throws Exception {

		UserRequestDTO request = new UserRequestDTO(
				"test@example.com",
				Role.ROLE_USER
		);

		UserResponseDTO response = new UserResponseDTO(
				1L,
				"test@example.com",
				Role.ROLE_USER,
				LocalDateTime.now(),
				LocalDateTime.now(),
				1L,
				1L
		);

		Mockito.when(createUserService.createUser(Mockito.any(UserRequestDTO.class)))
				.thenReturn(response);

		mockMvc.perform(post("/api/v1/users/registerUser")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.message").value("User created successfully"))
				.andExpect(jsonPath("$.data.email").value("test@example.com"))
				.andExpect(jsonPath("$.data.role").value(Role.ROLE_USER.name()));
	}

	//  2. VALIDATION FAILURE (@Valid)
	@Test
	void createUser_ShouldReturnBadRequest_WhenValidationFails() throws Exception {

		UserRequestDTO request = new UserRequestDTO(
				"",   // invalid email
				null  // invalid role
		);

		mockMvc.perform(post("/registerUser")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isInternalServerError());

		Mockito.verifyNoInteractions(createUserService);
	}

	// 3. SERVICE THROWS EXCEPTION
	@Test
	void createUser_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {

		UserRequestDTO request = new UserRequestDTO(
				"test@example.com",
				Role.ROLE_USER
		);

		Mockito.when(createUserService.createUser(Mockito.any(UserRequestDTO.class)))
				.thenThrow(new RuntimeException("Something went wrong"));

		mockMvc.perform(post("/registerUser")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isInternalServerError());
	}

	// 4. MALFORMED JSON INPUT
	@Test
	void createUser_ShouldReturnBadRequest_WhenMalformedJson() throws Exception {

		String invalidJson = "{ invalid json }";

		mockMvc.perform(post("/registerUser")
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalidJson))
				.andExpect(status().isInternalServerError());

		Mockito.verifyNoInteractions(createUserService);
	}
}
