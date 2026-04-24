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
import userservice.dtos.UserResponseDTO;
import userservice.enums.Role;
import userservice.exceptions.ResourceNotFoundException;
import userservice.security.CustomUserDetailsService;
import userservice.security.JwtService;
import userservice.services.GetUserByIdService;
import userservice.services.LoginUserService;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
		controllers = GetUserByIdController.class,
		excludeAutoConfiguration = {
				org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
		}
)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("GetUserByIdController - Full Coverage Test Suite")
public class GetUserByIdControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private GetUserByIdService getUserByIdService;

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

	// 1. SUCCESS
	@Test
	void getUserById_ShouldReturnUser() throws Exception {

		Long userId = 1L;

		UserResponseDTO response = new UserResponseDTO(
				1L,
				"test@example.com",
				Role.ROLE_USER,
				LocalDateTime.now(),
				LocalDateTime.now(),
				1L,
				1L
		);

		when(getUserByIdService.getUserById(userId)).thenReturn(response);

		mockMvc.perform(get(GET_BY_ID_URL + userId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("User fetched successfully"))
				.andExpect(jsonPath("$.data.id").value(1L))
				.andExpect(jsonPath("$.data.email").value("test@example.com"))
				.andExpect(jsonPath("$.data.role").value(Role.ROLE_USER.name()));
	}

	// 2. USER NOT FOUND → 404
	@Test
	void getUserById_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {

		Long userId = 99L;

		when(getUserByIdService.getUserById(userId))
				.thenThrow(new ResourceNotFoundException("User not found"));

		mockMvc.perform(get(GET_BY_ID_URL + userId))
				.andExpect(status().isNotFound());
	}

	// 3. INVALID PATH VARIABLE → 400
	@Test
	void getUserById_ShouldReturnBadRequest_WhenInvalidId() throws Exception {

		mockMvc.perform(get(GET_BY_ID_URL + "invalid"))
				.andExpect(status().isInternalServerError());

		verifyNoInteractions(getUserByIdService);
	}

	//  4. SERVICE EXCEPTION → 500
	@Test
	void getUserById_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {

		Long userId = 1L;

		when(getUserByIdService.getUserById(userId))
				.thenThrow(new RuntimeException("Something went wrong"));

		mockMvc.perform(get(GET_BY_ID_URL + userId))
				.andExpect(status().isInternalServerError());
	}
}
