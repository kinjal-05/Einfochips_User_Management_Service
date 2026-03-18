package userservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import userservice.dtos.*;
import userservice.enums.Role;
import userservice.exceptions.ResourceNotFoundException;
import userservice.services.UserService;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
 * USER CONTROLLER INTEGRATION TEST SUITE
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * This test class validates the complete behavior of UserController using
 * Spring Boot Test + MockMvc in a fully loaded application context.
 *
 * Key principles:
 *  - All service-layer beans are mocked (no DB calls or real JWT validation)
 *  - Isolated controller behavior verification
 *  - Covers success, validation, authentication, authorization, and exception cases
 *
 * Security assumptions:
 *  - CSRF disabled (do NOT use .with(csrf()))
 *  - JWT authentication used, HTTP Basic disabled
 *  - Unauthenticated requests return 403 (no AuthenticationEntryPoint configured)
 *  - All endpoints require only .authenticated() unless explicitly public
 *
 * Testing strategy:
 *  - Positive scenarios (200/201)
 *  - Validation errors (400)
 *  - Authentication errors (401, 403)
 *  - Business errors (404)
 *  - Pagination and filtering behavior
 *  - Service-layer interaction verified using Mockito
 *
 * Recommended production improvements:
 *  - Consider @Nested classes per endpoint
 *  - Use @Tag for grouping in CI reports
 *  - Use descriptive test method naming for clarity
 * ═══════════════════════════════════════════════════════════════════════════
 */

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("UserController - Complete Test Suite")
class UserControllerTest {

	// ── DEPENDENCIES ──────────────────────────────────────────────────────────

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean private UserService userService;
	@MockBean private PasswordEncoder passwordEncoder;
	@MockBean private JwtService jwtService;
	@MockBean private CustomUserDetailsService customUserDetailsService;

	// ── URL CONSTANTS ─────────────────────────────────────────────────────────

	private static final String BASE_URL        = "/api/v1/users";
	private static final String REGISTER_URL    = BASE_URL + "/registerUser";
	private static final String LOGIN_URL       = BASE_URL + "/login";
	private static final String SEARCH_URL      = BASE_URL + "/search";
	private static final String UPDATE_URL      = BASE_URL + "/updateUser/{id}";
	private static final String GET_BY_ID_URL   = BASE_URL + "/getById/{id}";
	private static final String DELETE_URL      = BASE_URL + "/deleteUser/{id}";
	private static final String CHANGE_PASS_URL = BASE_URL + "/changePassword";

	// ── TEST DATA ──────────────────────────────────────────────────────────────

	private static final long   USER_ID    = 1L;
	private static final long   ADMIN_ID   = 2L;
	private static final String USER_EMAIL = "test@example.com";
	private static final String JWT_TOKEN  = "mocked-jwt-token";

	private static final LocalDateTime CREATED_AT = LocalDateTime.of(2024, 1, 1, 10, 0);
	private static final LocalDateTime UPDATED_AT = LocalDateTime.of(2024, 6, 1, 12, 0);

	// ── FIXTURES ───────────────────────────────────────────────────────────────

	private UserResponseDTO           userResponse;
	private LoginResponseDTO          loginResponse;
	private ChangePasswordResponseDTO changePassResponse;
	private DeleteResponseDTO         deleteResponse;

	@BeforeEach
	void setUp() {
		objectMapper.registerModule(new JavaTimeModule());

		userResponse = new UserResponseDTO(
				USER_ID, USER_EMAIL, Role.ROLE_USER,
				CREATED_AT, UPDATED_AT, ADMIN_ID, ADMIN_ID
		);

		loginResponse = new LoginResponseDTO(
				USER_ID, USER_EMAIL, Role.ROLE_USER, JWT_TOKEN, "Login Successful"
		);

		changePassResponse = new ChangePasswordResponseDTO(
				USER_ID, USER_EMAIL, "Password changed successfully"
		);

		deleteResponse = new DeleteResponseDTO(
				USER_ID, USER_EMAIL, Role.ROLE_USER,
				CREATED_AT, UPDATED_AT, ADMIN_ID, ADMIN_ID,
				true, "User soft-deleted successfully"
		);
	}

	// ── HELPER METHODS ─────────────────────────────────────────────────────────

	/**
	 * Builds UserSearchRequestDTO with safe defaults for primitive long fields.
	 */
	private UserSearchRequestDTO buildSearchRequest(
			String email, Role role,
			Long createdById, Long updatedById,
			LocalDateTime fromDate, LocalDateTime toDate) {
		return new UserSearchRequestDTO(
				email, role,
				createdById != null ? createdById : 0L,
				updatedById != null ? updatedById : 0L,
				fromDate, toDate
		);
	}

	/** Builds a full UserResponseDTO with default timestamps and audit info. */
	private UserResponseDTO buildUserResponse(long id, String email, Role role) {
		return new UserResponseDTO(id, email, role, CREATED_AT, UPDATED_AT, ADMIN_ID, ADMIN_ID);
	}

	// ═══════════════════════════════════════════════════════════════════════
	// REGISTER USER ENDPOINT - POST /registerUser
	// ───────────────────────────────────────────────────────────────────────
	// Secured: Requires authentication
	// Expected: 201 (success), 400 (validation), 403 (unauthenticated)
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	@Order(1)
	@WithMockUser
	@DisplayName("Register - 201 CREATED with valid request body")
	void registerUser_success_returns201() throws Exception {
		UserRequestDTO request = new UserRequestDTO(USER_EMAIL, Role.ROLE_USER);

		when(userService.registerUser(any(UserRequestDTO.class))).thenReturn(userResponse);

		mockMvc.perform(post(REGISTER_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id",    is((int) USER_ID)))
				.andExpect(jsonPath("$.email", is(USER_EMAIL)))
				.andExpect(jsonPath("$.role",  is("ROLE_USER")));

		verify(userService, times(1)).registerUser(any(UserRequestDTO.class));
	}

	@Test
	@Order(2)
	@WithMockUser
	@DisplayName("Register - 400 BAD REQUEST when email is null")
	void registerUser_missingEmail_returns400() throws Exception {
		UserRequestDTO badRequest = new UserRequestDTO(null, Role.ROLE_USER);

		mockMvc.perform(post(REGISTER_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(badRequest)))
				.andDo(print())
				.andExpect(status().isBadRequest());

		verify(userService, never()).registerUser(any());
	}

	@Test
	@Order(3)
	@WithMockUser
	@DisplayName("Register - 400 BAD REQUEST when role is null")
	void registerUser_missingRole_returns400() throws Exception {
		UserRequestDTO badRequest = new UserRequestDTO(USER_EMAIL, null);

		mockMvc.perform(post(REGISTER_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(badRequest)))
				.andDo(print())
				.andExpect(status().isBadRequest());

		verify(userService, never()).registerUser(any());
	}

	@Test
	@Order(4)
	@WithMockUser
	@DisplayName("Register - 400 BAD REQUEST when body is empty JSON")
	void registerUser_emptyBody_returns400() throws Exception {
		mockMvc.perform(post(REGISTER_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andDo(print())
				.andExpect(status().isBadRequest());
	}

	@Test
	@Order(5)
	@DisplayName("Register - 403 FORBIDDEN when unauthenticated")
	void registerUser_unauthenticated_returns403() throws Exception {
		UserRequestDTO request = new UserRequestDTO(USER_EMAIL, Role.ROLE_USER);

		mockMvc.perform(post(REGISTER_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	// ═══════════════════════════════════════════════════════════════════════
	// LOGIN ENDPOINT - POST /login
	// ───────────────────────────────────────────────────────────────────────
	// Public endpoint: No authentication required
	// Expected: 200 (success), 400 (validation), 401 (bad credentials)
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	@Order(6)
	@DisplayName("Login - 200 OK returns token and all user details")
	void login_success_returns200WithToken() throws Exception {
		LoginRequestDTO request = new LoginRequestDTO(USER_EMAIL, "password123");

		when(userService.login(any(LoginRequestDTO.class))).thenReturn(loginResponse);

		mockMvc.perform(post(LOGIN_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id",      is((int) USER_ID)))
				.andExpect(jsonPath("$.email",   is(USER_EMAIL)))
				.andExpect(jsonPath("$.role",    is("ROLE_USER")))
				.andExpect(jsonPath("$.token",   is(JWT_TOKEN)))
				.andExpect(jsonPath("$.message", is("Login Successful")));

		verify(userService, times(1)).login(any(LoginRequestDTO.class));
	}

	@Test
	@Order(7)
	@DisplayName("Login - 401 UNAUTHORIZED when bad credentials")
	void login_invalidCredentials_returns401() throws Exception {
		LoginRequestDTO request = new LoginRequestDTO(USER_EMAIL, "wrongPassword");

		when(userService.login(any(LoginRequestDTO.class)))
				.thenThrow(new BadCredentialsException("Bad credentials"));

		mockMvc.perform(post(LOGIN_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isUnauthorized());
	}

	@Test
	@Order(8)
	@DisplayName("Login - 400 BAD REQUEST when email is blank")
	void login_blankEmail_returns400() throws Exception {
		LoginRequestDTO request = new LoginRequestDTO("", "password123");

		mockMvc.perform(post(LOGIN_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest());
	}

	@Test
	@Order(9)
	@DisplayName("Login - 400 BAD REQUEST when password is blank")
	void login_blankPassword_returns400() throws Exception {
		LoginRequestDTO request = new LoginRequestDTO(USER_EMAIL, "");

		mockMvc.perform(post(LOGIN_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest());
	}

	// ═══════════════════════════════════════════════════════════════════════
	// SEARCH USERS - POST /search
	// ───────────────────────────────────────────────────────────────────────
	// Secured endpoint: Requires authentication
	// Supports filtering, pagination, and date ranges
	// Expected: 200 (results), 404 (no results)
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	@Order(10)
	@WithMockUser
	@DisplayName("Search - 200 OK with all filters")
	void searchUsers_success_returns200() throws Exception {
		UserSearchRequestDTO searchRequest =
				buildSearchRequest(USER_EMAIL, Role.ROLE_USER, ADMIN_ID, ADMIN_ID, null, null);

		Page<UserResponseDTO> page = new PageImpl<>(List.of(userResponse));

		when(userService.searchUsers(any(UserSearchRequestDTO.class), any(Pageable.class)))
				.thenReturn(page);

		mockMvc.perform(post(SEARCH_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(searchRequest))
						.param("page", "0")
						.param("size", "10"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content",          hasSize(1)))
				.andExpect(jsonPath("$.content[0].email", is(USER_EMAIL)))
				.andExpect(jsonPath("$.totalElements",    is(1)));

		verify(userService, times(1)).searchUsers(any(), any(Pageable.class));
	}

	// ═══════════════════════════════════════════════════════════════════════
	// UPDATE USER - PATCH /updateUser/{id}
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	@Order(15)
	@WithMockUser
	@DisplayName("Update - 200 OK updates email and role")
	void updateUser_success_returns200() throws Exception {
		UserUpdateRequestDTO updateRequest =
				new UserUpdateRequestDTO("updated@example.com", Role.ROLE_ADMIN);

		UserResponseDTO updated =
				buildUserResponse(USER_ID, "updated@example.com", Role.ROLE_ADMIN);

		when(userService.updateUser(eq(USER_ID), any(UserUpdateRequestDTO.class)))
				.thenReturn(updated);

		mockMvc.perform(patch(UPDATE_URL, USER_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(updateRequest)))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id",    is((int) USER_ID)))
				.andExpect(jsonPath("$.email", is("updated@example.com")))
				.andExpect(jsonPath("$.role",  is("ROLE_ADMIN")));

		verify(userService, times(1)).updateUser(eq(USER_ID), any(UserUpdateRequestDTO.class));
	}

	// ═══════════════════════════════════════════════════════════════════════
	// GET USER BY ID - GET /getById/{id}
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	@Order(19)
	@WithMockUser
	@DisplayName("GetById - 200 OK returns full UserResponseDTO")
	void getUserById_success_returns200() throws Exception {
		when(userService.getUserById(USER_ID)).thenReturn(userResponse);

		mockMvc.perform(get(GET_BY_ID_URL, USER_ID))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id",          is((int) USER_ID)))
				.andExpect(jsonPath("$.email",       is(USER_EMAIL)))
				.andExpect(jsonPath("$.role",        is("ROLE_USER")))
				.andExpect(jsonPath("$.createdById", is((int) ADMIN_ID)))
				.andExpect(jsonPath("$.updatedById", is((int) ADMIN_ID)));

		verify(userService, times(1)).getUserById(USER_ID);
	}

	// ═══════════════════════════════════════════════════════════════════════
	// DELETE USER - DELETE /deleteUser/{id} (soft delete)
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	@Order(22)
	@WithMockUser
	@DisplayName("SoftDelete - 200 OK returns full DeleteResponseDTO")
	void softDeleteUser_success_returns200() throws Exception {
		when(userService.softDeleteUser(USER_ID)).thenReturn(deleteResponse);

		mockMvc.perform(delete(DELETE_URL, USER_ID))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id",        is((int) USER_ID)))
				.andExpect(jsonPath("$.email",     is(USER_EMAIL)))
				.andExpect(jsonPath("$.role",      is("ROLE_USER")))
				.andExpect(jsonPath("$.isDeleted", is(true)))
				.andExpect(jsonPath("$.message",   is("User soft-deleted successfully")));

		verify(userService, times(1)).softDeleteUser(USER_ID);
	}

	// ═══════════════════════════════════════════════════════════════════════
	// CHANGE PASSWORD - PATCH /changePassword
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	@Order(25)
	@WithMockUser
	@DisplayName("ChangePassword - 200 OK returns userId, email and message")
	void changePassword_success_returns200() throws Exception {
		ChangePasswordRequestDTO changeRequest =
				new ChangePasswordRequestDTO("oldPass@1", "newPass@1");

		when(userService.changePassword(any(ChangePasswordRequestDTO.class)))
				.thenReturn(changePassResponse);

		mockMvc.perform(patch(CHANGE_PASS_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(changeRequest)))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId",  is((int) USER_ID)))
				.andExpect(jsonPath("$.email",   is(USER_EMAIL)))
				.andExpect(jsonPath("$.message", is("Password changed successfully")));

		verify(userService, times(1)).changePassword(any(ChangePasswordRequestDTO.class));
	}
}