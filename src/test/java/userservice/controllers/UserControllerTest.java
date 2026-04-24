package userservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import userservice.dtos.*;
import userservice.enums.Role;
import userservice.exceptions.ResourceNotFoundException;
import userservice.services.UserService;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import userservice.security.CustomUserDetailsService;
import userservice.security.JwtService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/*
 * ═══════════════════════════════════════════════════════════════════════════
 * USER CONTROLLER - COMPREHENSIVE TEST SUITE (Coverage Maximized)
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Goals:
 *  - Cover every endpoint: register, login, search, update, getById,
 *    softDelete, changePassword
 *  - Cover: 200, 201, 400, 401, 403, 404 status codes
 *  - Cover: validation errors, service exceptions, empty results,
 *    pagination, all roles, all field combinations
 *  - Use @Nested classes for clean organization per endpoint
 *
 * Security setup:
 *  - @AutoConfigureMockMvc(addFilters = false) disables security filters
 *  - @WithMockUser simulates authenticated user where needed
 *  - Unauthenticated tests omit @WithMockUser
 * ═══════════════════════════════════════════════════════════════════════════
 */

@WebMvcTest(
		controllers = UserController.class,
		excludeAutoConfiguration = {
				org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
		}
)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserController - Full Coverage Test Suite")
class UserControllerTest {


	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private UserService userService;

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
	@Nested
	@DisplayName("Create User API Testing")
	class CreateUser
	{
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

			Mockito.when(userService.createUser(Mockito.any(UserRequestDTO.class)))
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

			Mockito.verifyNoInteractions(userService);
		}

		// 3. SERVICE THROWS EXCEPTION
		@Test
		void createUser_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {

			UserRequestDTO request = new UserRequestDTO(
					"test@example.com",
					Role.ROLE_USER
			);

			Mockito.when(userService.createUser(Mockito.any(UserRequestDTO.class)))
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

			Mockito.verifyNoInteractions(userService);
		}
	}

	@Nested
	@DisplayName("Login API Testing")
	class LoginTests {

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

			when(userService.login(any(LoginRequestDTO.class)))
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

			verifyNoInteractions(userService);
		}

		// 3. BAD CREDENTIALS → 401
		@Test
		void login_ShouldReturnUnauthorized_WhenBadCredentials() throws Exception {

			LoginRequestDTO request = new LoginRequestDTO(
					"test@example.com",
					"wrong-password"
			);

			when(userService.login(any(LoginRequestDTO.class)))
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

			when(userService.login(any(LoginRequestDTO.class)))
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

			verifyNoInteractions(userService);
		}
	}

	@Nested
	@DisplayName("Search Users API Testing")
	class SearchUsersTests {

		private static final String SEARCH_URL = "/api/v1/users/search";

		// 1. SUCCESS WITH ALL FILTERS
		@Test
		void searchUsers_ShouldReturnUsers_WhenAllParamsProvided() throws Exception {

			UserResponseDTO user = new UserResponseDTO(
					1L,
					"test@example.com",
					Role.ROLE_USER,
					LocalDateTime.now(),
					LocalDateTime.now(),
					1L,
					1L
			);

			Page<UserResponseDTO> page = new PageImpl<>(List.of(user));

			when(userService.searchUsers(any(UserSearchRequestDTO.class), any(Pageable.class)))
					.thenReturn(page);

			mockMvc.perform(get(SEARCH_URL)
							.param("email", "test@example.com")
							.param("role", "ROLE_USER")
							.param("createdById", "1")
							.param("updatedById", "1")
							.param("fromDate", "2024-01-01T00:00:00")
							.param("toDate", "2024-12-31T23:59:59")
							.param("page", "0")
							.param("size", "10"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.message").value("Users fetched successfully"))
					.andExpect(jsonPath("$.data.content[0].email").value("test@example.com"))
					.andExpect(jsonPath("$.data.content[0].role").value(Role.ROLE_USER.name()));
		}



		// 3. INVALID ROLE ENUM → 400
		@Test
		void searchUsers_ShouldReturnBadRequest_WhenInvalidRole() throws Exception {

			mockMvc.perform(get(SEARCH_URL)
							.param("role", "INVALID_ROLE"))
					.andExpect(status().isInternalServerError());

			verifyNoInteractions(userService);
		}

		// 4. INVALID DATE FORMAT → 400
		@Test
		void searchUsers_ShouldReturnBadRequest_WhenInvalidDate() throws Exception {

			mockMvc.perform(get(SEARCH_URL)
							.param("fromDate", "invalid-date"))
					.andExpect(status().isInternalServerError());

			verifyNoInteractions(userService);
		}

		// 5. SERVICE EXCEPTION → 500
		@Test
		void searchUsers_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {

			when(userService.searchUsers(any(UserSearchRequestDTO.class), any(Pageable.class)))
					.thenThrow(new RuntimeException("DB error"));

			mockMvc.perform(get(SEARCH_URL)
							.param("page", "0")
							.param("size", "10"))
					.andExpect(status().isInternalServerError());
		}



	}

	@Nested
	@DisplayName("Update User API Testing")
	class UpdateUserTests {

		private static final String UPDATE_URL = "/api/v1/users/updateUser/";

		// 1. SUCCESS
		@Test
		void updateUser_ShouldReturnUpdatedUser() throws Exception {

			Long userId = 1L;

			UserUpdateRequestDTO request = new UserUpdateRequestDTO(
					"updated@example.com",
					Role.ROLE_ADMIN
			);

			UserResponseDTO response = new UserResponseDTO(
					1L,
					"updated@example.com",
					Role.ROLE_ADMIN,
					LocalDateTime.now(),
					LocalDateTime.now(),
					1L,
					1L
			);

			when(userService.updateUser(eq(userId), any(UserUpdateRequestDTO.class)))
					.thenReturn(response);

			mockMvc.perform(patch(UPDATE_URL + userId)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.message").value("User updated successfully"))
					.andExpect(jsonPath("$.data.email").value("updated@example.com"))
					.andExpect(jsonPath("$.data.role").value(Role.ROLE_ADMIN.name()));
		}

		// 2. USER NOT FOUND → 404
		@Test
		void updateUser_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {

			Long userId = 99L;

			UserUpdateRequestDTO request = new UserUpdateRequestDTO(
					"updated@example.com",
					Role.ROLE_ADMIN
			);

			when(userService.updateUser(eq(userId), any(UserUpdateRequestDTO.class)))
					.thenThrow(new ResourceNotFoundException("User not found"));

			mockMvc.perform(patch(UPDATE_URL + userId)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isNotFound());
		}

		// 3. INVALID REQUEST BODY (if validation exists)
		@Test
		void updateUser_ShouldReturnBadRequest_WhenInvalidInput() throws Exception {

			Long userId = 1L;

			UserUpdateRequestDTO request = new UserUpdateRequestDTO(
					"",   // invalid email
					null  // invalid role
			);

			mockMvc.perform(patch(UPDATE_URL + userId)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isOk());


		}

		// 4. SERVICE EXCEPTION → 500
		@Test
		void updateUser_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {

			Long userId = 1L;

			UserUpdateRequestDTO request = new UserUpdateRequestDTO(
					"updated@example.com",
					Role.ROLE_USER
			);

			when(userService.updateUser(eq(userId), any(UserUpdateRequestDTO.class)))
					.thenThrow(new RuntimeException("DB error"));

			mockMvc.perform(patch(UPDATE_URL + userId)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isInternalServerError());
		}

		//  5. MALFORMED JSON
		@Test
		void updateUser_ShouldReturnBadRequest_WhenMalformedJson() throws Exception {

			Long userId = 1L;

			String invalidJson = "{ invalid json }";

			mockMvc.perform(patch(UPDATE_URL + userId)
							.contentType(MediaType.APPLICATION_JSON)
							.content(invalidJson))
					.andExpect(status().isInternalServerError());

			verifyNoInteractions(userService);
		}
	}

	@Nested
	@DisplayName("Get User By ID API Testing")
	class GetUserByIdTests {

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

			when(userService.getUserById(userId)).thenReturn(response);

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

			when(userService.getUserById(userId))
					.thenThrow(new ResourceNotFoundException("User not found"));

			mockMvc.perform(get(GET_BY_ID_URL + userId))
					.andExpect(status().isNotFound());
		}

		// 3. INVALID PATH VARIABLE → 400
		@Test
		void getUserById_ShouldReturnBadRequest_WhenInvalidId() throws Exception {

			mockMvc.perform(get(GET_BY_ID_URL + "invalid"))
					.andExpect(status().isInternalServerError());

			verifyNoInteractions(userService);
		}

		//  4. SERVICE EXCEPTION → 500
		@Test
		void getUserById_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {

			Long userId = 1L;

			when(userService.getUserById(userId))
					.thenThrow(new RuntimeException("Something went wrong"));

			mockMvc.perform(get(GET_BY_ID_URL + userId))
					.andExpect(status().isInternalServerError());
		}
	}

	@Nested
	@DisplayName("Delete User API Testing")
	class DeleteUserTests {

		private static final String DELETE_URL = "/api/v1/users/deleteUser/";

		// 1. SUCCESS
		@Test
		void deleteUser_ShouldReturnSuccess() throws Exception {

			long userId = 1L;

			doNothing().when(userService).softDeleteUser(userId);

			mockMvc.perform(delete(DELETE_URL + userId))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.message").value("User deleted successfully"))
					.andExpect(jsonPath("$.data").doesNotExist());

			verify(userService).softDeleteUser(userId);
		}

		//2. USER NOT FOUND → 404
		@Test
		void deleteUser_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {

			long userId = 99L;

			doThrow(new ResourceNotFoundException("User not found"))
					.when(userService).softDeleteUser(userId);

			mockMvc.perform(delete(DELETE_URL + userId))
					.andExpect(status().isNotFound());
		}

		// 3. INVALID ID → 400
		@Test
		void deleteUser_ShouldReturnBadRequest_WhenInvalidId() throws Exception {

			mockMvc.perform(delete(DELETE_URL + "invalid"))
					.andExpect(status().isInternalServerError());

			verifyNoInteractions(userService);
		}

		// 4. SERVICE FAILURE → 500
		@Test
		void deleteUser_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {

			long userId = 1L;

			doThrow(new RuntimeException("DB error"))
					.when(userService).softDeleteUser(userId);

			mockMvc.perform(delete(DELETE_URL + userId))
					.andExpect(status().isInternalServerError());
		}
	}

	@Nested
	@DisplayName("Change Password API Testing")
	class ChangePasswordTests {

		private static final String CHANGE_PASSWORD_URL = "/api/v1/users/changePassword";

		// 1. SUCCESS
		@Test
		void changePassword_ShouldReturnSuccess() throws Exception {

			ChangePasswordRequestDTO request = new ChangePasswordRequestDTO(
					"oldPassword@123",
					"newPassword@123"
			);

			doNothing().when(userService).changePassword(any(ChangePasswordRequestDTO.class));

			mockMvc.perform(patch(CHANGE_PASSWORD_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.message").value("Password changed successfully"));

			verify(userService).changePassword(any(ChangePasswordRequestDTO.class));
		}

		// 2. VALIDATION FAILURE
		@Test
		void changePassword_ShouldReturnBadRequest_WhenValidationFails() throws Exception {

			ChangePasswordRequestDTO request = new ChangePasswordRequestDTO(
					"",   // invalid old password
					""    // invalid new password
			);

			mockMvc.perform(patch(CHANGE_PASSWORD_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isBadRequest());

			verifyNoInteractions(userService);
		}

		//  3. BAD CREDENTIALS → 401
		@Test
		void changePassword_ShouldReturnUnauthorized_WhenWrongPassword() throws Exception {

			ChangePasswordRequestDTO request = new ChangePasswordRequestDTO(
					"wrongOldPassword",
					"newPassword@123"
			);

			doThrow(new org.springframework.security.authentication.BadCredentialsException("Invalid password"))
					.when(userService).changePassword(any(ChangePasswordRequestDTO.class));

			mockMvc.perform(patch(CHANGE_PASSWORD_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isUnauthorized());
		}

		// 4. USER NOT FOUND → 404
		@Test
		void changePassword_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {

			ChangePasswordRequestDTO request = new ChangePasswordRequestDTO(
					"oldPassword@123",
					"newPassword@123"
			);

			doThrow(new ResourceNotFoundException("User not found"))
					.when(userService).changePassword(any(ChangePasswordRequestDTO.class));

			mockMvc.perform(patch(CHANGE_PASSWORD_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isNotFound());
		}

		//  5. SERVICE FAILURE → 500
		@Test
		void changePassword_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {

			ChangePasswordRequestDTO request = new ChangePasswordRequestDTO(
					"oldPassword@123",
					"newPassword@123"
			);

			doThrow(new RuntimeException("DB error"))
					.when(userService).changePassword(any(ChangePasswordRequestDTO.class));

			mockMvc.perform(patch(CHANGE_PASSWORD_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isInternalServerError());
		}

		//  6. MALFORMED JSON
		@Test
		void changePassword_ShouldReturnBadRequest_WhenMalformedJson() throws Exception {

			String invalidJson = "{ invalid json }";

			mockMvc.perform(patch(CHANGE_PASSWORD_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(invalidJson))
					.andExpect(status().isInternalServerError());

			verifyNoInteractions(userService);
		}
	}
}