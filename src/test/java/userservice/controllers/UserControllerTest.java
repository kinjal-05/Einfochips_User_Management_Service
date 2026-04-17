package userservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("UserController - Full Coverage Test Suite")
class UserControllerTest {


	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean private UserService userService;
	@MockBean private PasswordEncoder passwordEncoder;
	@MockBean private JwtService jwtService;
	@MockBean private CustomUserDetailsService customUserDetailsService;


	private static final String BASE_URL        = "/api/v1/users";
	private static final String REGISTER_URL    = BASE_URL + "/registerUser";
	private static final String LOGIN_URL       = BASE_URL + "/login";
	private static final String SEARCH_URL      = BASE_URL + "/search";
	private static final String UPDATE_URL      = BASE_URL + "/updateUser/{id}";
	private static final String GET_BY_ID_URL   = BASE_URL + "/getById/{id}";
	private static final String DELETE_URL      = BASE_URL + "/deleteUser/{id}";
	private static final String CHANGE_PASS_URL = BASE_URL + "/changePassword";


	private static final long   USER_ID        = 1L;
	private static final long   ADMIN_ID       = 2L;
	private static final String USER_EMAIL     = "test@example.com";
	private static final String ADMIN_EMAIL    = "admin@example.com";
	private static final String JWT_TOKEN      = "mocked-jwt-token";
	private static final String VALID_PASSWORD = "OldPass@1";
	private static final String NEW_PASSWORD   = "NewPass@1";

	private static final LocalDateTime CREATED_AT = LocalDateTime.of(2024, 1, 1, 10, 0);
	private static final LocalDateTime UPDATED_AT = LocalDateTime.of(2024, 6, 1, 12, 0);

	private UserResponseDTO           userResponse;
	private UserResponseDTO           adminResponse;
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

		adminResponse = new UserResponseDTO(
				ADMIN_ID, ADMIN_EMAIL, Role.ROLE_ADMIN,
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

	private UserResponseDTO buildUserResponse(long id, String email, Role role) {
		return new UserResponseDTO(id, email, role, CREATED_AT, UPDATED_AT, ADMIN_ID, ADMIN_ID);
	}

	private String toJson(Object obj) throws Exception {
		return objectMapper.writeValueAsString(obj);
	}


	@Nested
	@DisplayName("Register User - POST /registerUser")
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class RegisterUserTests {

		@Test
		@Order(1)
		@WithMockUser
		@DisplayName("201 CREATED - valid ROLE_USER request")
		void registerUser_validUser_returns201() throws Exception {
			UserRequestDTO request = new UserRequestDTO(USER_EMAIL, Role.ROLE_USER);
			when(userService.registerUser(any(UserRequestDTO.class))).thenReturn(userResponse);

			mockMvc.perform(post(REGISTER_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(request)))
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
		@DisplayName("201 CREATED - valid ROLE_ADMIN request")
		void registerUser_validAdmin_returns201() throws Exception {
			UserRequestDTO request = new UserRequestDTO(ADMIN_EMAIL, Role.ROLE_ADMIN);
			when(userService.registerUser(any(UserRequestDTO.class))).thenReturn(adminResponse);

			mockMvc.perform(post(REGISTER_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(request)))
					.andDo(print())
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.email", is(ADMIN_EMAIL)))
					.andExpect(jsonPath("$.role",  is("ROLE_ADMIN")));

			verify(userService, times(1)).registerUser(any(UserRequestDTO.class));
		}

		@Test
		@Order(3)
		@WithMockUser
		@DisplayName("400 BAD REQUEST - email is null")
		void registerUser_nullEmail_returns400() throws Exception {
			UserRequestDTO request = new UserRequestDTO(null, Role.ROLE_USER);

			mockMvc.perform(post(REGISTER_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(request)))
					.andDo(print())
					.andExpect(status().isBadRequest());

			verify(userService, never()).registerUser(any());
		}

		@Test
		@Order(4)
		@WithMockUser
		@DisplayName("400 BAD REQUEST - email is blank string")
		void registerUser_blankEmail_returns400() throws Exception {
			UserRequestDTO request = new UserRequestDTO("", Role.ROLE_USER);

			mockMvc.perform(post(REGISTER_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(request)))
					.andDo(print())
					.andExpect(status().isBadRequest());

			verify(userService, never()).registerUser(any());
		}

		@Test
		@Order(5)
		@WithMockUser
		@DisplayName("400 BAD REQUEST - email format invalid")
		void registerUser_invalidEmailFormat_returns400() throws Exception {
			UserRequestDTO request = new UserRequestDTO("not-an-email", Role.ROLE_USER);

			mockMvc.perform(post(REGISTER_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(request)))
					.andDo(print())
					.andExpect(status().isBadRequest());

			verify(userService, never()).registerUser(any());
		}

		@Test
		@Order(6)
		@WithMockUser
		@DisplayName("400 BAD REQUEST - role is null")
		void registerUser_nullRole_returns400() throws Exception {
			UserRequestDTO request = new UserRequestDTO(USER_EMAIL, null);

			mockMvc.perform(post(REGISTER_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(request)))
					.andDo(print())
					.andExpect(status().isBadRequest());

			verify(userService, never()).registerUser(any());
		}

		@Test
		@Order(7)
		@WithMockUser
		@DisplayName("400 BAD REQUEST - completely empty JSON body")
		void registerUser_emptyBody_returns400() throws Exception {
			mockMvc.perform(post(REGISTER_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{}"))
					.andDo(print())
					.andExpect(status().isBadRequest());

			verify(userService, never()).registerUser(any());
		}

//		@Test
//		@Order(8)
//		@WithMockUser
//		@DisplayName("400 BAD REQUEST - missing Content-Type header")
//		void registerUser_missingContentType_returns400() throws Exception {
//			mockMvc.perform(post(REGISTER_URL)
//							.content(toJson(new UserRequestDTO(USER_EMAIL, Role.ROLE_USER))))
//					.andDo(print())
//					.andExpect(status().isBadRequest());
//		}

		@Test
		@Order(9)
		@WithMockUser
		@DisplayName("Response body contains createdById and updatedById")
		void registerUser_responseContainsAuditFields() throws Exception {
			UserRequestDTO request = new UserRequestDTO(USER_EMAIL, Role.ROLE_USER);
			when(userService.registerUser(any())).thenReturn(userResponse);

			mockMvc.perform(post(REGISTER_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(request)))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.createdById", is((int) ADMIN_ID)))
					.andExpect(jsonPath("$.updatedById", is((int) ADMIN_ID)));
		}

		@Test
		@Order(10)
		@WithMockUser
		@DisplayName("Service called exactly once per request")
		void registerUser_serviceCalledOnce() throws Exception {
			when(userService.registerUser(any())).thenReturn(userResponse);

			mockMvc.perform(post(REGISTER_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(new UserRequestDTO(USER_EMAIL, Role.ROLE_USER))))
					.andExpect(status().isCreated());

			verify(userService, times(1)).registerUser(any());
			verifyNoMoreInteractions(userService);
		}
	}


	@Nested
	@DisplayName("Login - POST /login")
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class LoginTests {

		@Test
		@Order(1)
		@DisplayName("200 OK - valid credentials return token and user details")
		void login_validCredentials_returns200WithToken() throws Exception {
			LoginRequestDTO request = new LoginRequestDTO(USER_EMAIL, VALID_PASSWORD);
			when(userService.login(any(LoginRequestDTO.class))).thenReturn(loginResponse);

			mockMvc.perform(post(LOGIN_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(request)))
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
		@Order(2)
		@DisplayName("200 OK - admin login returns ROLE_ADMIN")
		void login_adminCredentials_returnsAdminRole() throws Exception {
			LoginRequestDTO request = new LoginRequestDTO(ADMIN_EMAIL, VALID_PASSWORD);
			LoginResponseDTO adminLogin = new LoginResponseDTO(
					ADMIN_ID, ADMIN_EMAIL, Role.ROLE_ADMIN, JWT_TOKEN, "Login Successful"
			);
			when(userService.login(any())).thenReturn(adminLogin);

			mockMvc.perform(post(LOGIN_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(request)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.role", is("ROLE_ADMIN")));
		}

		@Test
		@Order(3)
		@DisplayName("401 UNAUTHORIZED - wrong password throws BadCredentialsException")
		void login_wrongPassword_returns401() throws Exception {
			LoginRequestDTO request = new LoginRequestDTO(USER_EMAIL, "wrongPass");
			when(userService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

			mockMvc.perform(post(LOGIN_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(request)))
					.andDo(print())
					.andExpect(status().isUnauthorized());
		}

		@Test
		@Order(4)
		@DisplayName("401 UNAUTHORIZED - unknown email throws BadCredentialsException")
		void login_unknownEmail_returns401() throws Exception {
			LoginRequestDTO request = new LoginRequestDTO("unknown@mail.com", VALID_PASSWORD);
			when(userService.login(any())).thenThrow(new BadCredentialsException("User not found"));

			mockMvc.perform(post(LOGIN_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(request)))
					.andExpect(status().isUnauthorized());
		}

		@Test
		@Order(5)
		@DisplayName("400 BAD REQUEST - blank email")
		void login_blankEmail_returns400() throws Exception {
			LoginRequestDTO request = new LoginRequestDTO("", VALID_PASSWORD);

			mockMvc.perform(post(LOGIN_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(request)))
					.andDo(print())
					.andExpect(status().isBadRequest());

			verify(userService, never()).login(any());
		}

		@Test
		@Order(6)
		@DisplayName("400 BAD REQUEST - null email")
		void login_nullEmail_returns400() throws Exception {
			LoginRequestDTO request = new LoginRequestDTO(null, VALID_PASSWORD);

			mockMvc.perform(post(LOGIN_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(request)))
					.andExpect(status().isBadRequest());

			verify(userService, never()).login(any());
		}

		@Test
		@Order(7)
		@DisplayName("400 BAD REQUEST - blank password")
		void login_blankPassword_returns400() throws Exception {
			LoginRequestDTO request = new LoginRequestDTO(USER_EMAIL, "");

			mockMvc.perform(post(LOGIN_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(request)))
					.andDo(print())
					.andExpect(status().isBadRequest());

			verify(userService, never()).login(any());
		}

		@Test
		@Order(8)
		@DisplayName("400 BAD REQUEST - null password")
		void login_nullPassword_returns400() throws Exception {
			LoginRequestDTO request = new LoginRequestDTO(USER_EMAIL, null);

			mockMvc.perform(post(LOGIN_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(request)))
					.andExpect(status().isBadRequest());

			verify(userService, never()).login(any());
		}

		@Test
		@Order(9)
		@DisplayName("400 BAD REQUEST - empty JSON body")
		void login_emptyBody_returns400() throws Exception {
			mockMvc.perform(post(LOGIN_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{}"))
					.andExpect(status().isBadRequest());
		}

		@Test
		@Order(10)
		@DisplayName("400 BAD REQUEST - invalid email format")
		void login_invalidEmailFormat_returns400() throws Exception {
			LoginRequestDTO request = new LoginRequestDTO("invalid-email", VALID_PASSWORD);

			mockMvc.perform(post(LOGIN_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(request)))
					.andExpect(status().isBadRequest());
		}
	}


	@Nested
	@DisplayName("Search Users - POST /search")
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class SearchUserTests {

		@Test
		@Order(1)
		@WithMockUser
		@DisplayName("200 OK - search with all filters returns matching page")
		void search_allFilters_returns200() throws Exception {
			UserSearchRequestDTO req = buildSearchRequest(
					USER_EMAIL, Role.ROLE_USER, ADMIN_ID, ADMIN_ID, null, null);
			Page<UserResponseDTO> page = new PageImpl<>(List.of(userResponse));
			when(userService.searchUsers(any(), any(Pageable.class))).thenReturn(page);

			mockMvc.perform(post(SEARCH_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(req))
							.param("page", "0")
							.param("size", "10"))
					.andDo(print())
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content",          hasSize(1)))
					.andExpect(jsonPath("$.content[0].email", is(USER_EMAIL)))
					.andExpect(jsonPath("$.totalElements",    is(1)));
		}

		@Test
		@Order(2)
		@WithMockUser
		@DisplayName("200 OK - search with no filters returns all users")
		void search_noFilters_returnsAllUsers() throws Exception {
			UserSearchRequestDTO req = buildSearchRequest(null, null, null, null, null, null);
			Page<UserResponseDTO> page = new PageImpl<>(List.of(userResponse, adminResponse));
			when(userService.searchUsers(any(), any())).thenReturn(page);

			mockMvc.perform(post(SEARCH_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(req)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content",       hasSize(2)))
					.andExpect(jsonPath("$.totalElements", is(2)));
		}

		@Test
		@Order(3)
		@WithMockUser
		@DisplayName("200 OK - search returns empty page when no match")
		void search_noMatch_returnsEmptyPage() throws Exception {
			UserSearchRequestDTO req = buildSearchRequest(
					"noone@example.com", null, null, null, null, null);
			Page<UserResponseDTO> emptyPage = new PageImpl<>(Collections.emptyList());
			when(userService.searchUsers(any(), any())).thenReturn(emptyPage);

			mockMvc.perform(post(SEARCH_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(req)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content",       hasSize(0)))
					.andExpect(jsonPath("$.totalElements", is(0)));
		}

		@Test
		@Order(4)
		@WithMockUser
		@DisplayName("200 OK - search with date range filters")
		void search_withDateRange_returns200() throws Exception {
			LocalDateTime from = LocalDateTime.of(2024, 1, 1, 0, 0);
			LocalDateTime to   = LocalDateTime.of(2024, 12, 31, 23, 59);
			UserSearchRequestDTO req = buildSearchRequest(null, null, null, null, from, to);
			Page<UserResponseDTO> page = new PageImpl<>(List.of(userResponse));
			when(userService.searchUsers(any(), any())).thenReturn(page);

			mockMvc.perform(post(SEARCH_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(req)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content", hasSize(1)));
		}

		@Test
		@Order(5)
		@WithMockUser
		@DisplayName("200 OK - search by ROLE_ADMIN returns admin users only")
		void search_byAdminRole_returnsAdmins() throws Exception {
			UserSearchRequestDTO req = buildSearchRequest(null, Role.ROLE_ADMIN, null, null, null, null);
			Page<UserResponseDTO> page = new PageImpl<>(List.of(adminResponse));
			when(userService.searchUsers(any(), any())).thenReturn(page);

			mockMvc.perform(post(SEARCH_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(req)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content[0].role", is("ROLE_ADMIN")));
		}

		@Test
		@Order(6)
		@WithMockUser
		@DisplayName("200 OK - pagination params respected (page=1, size=5)")
		void search_pagination_params_respected() throws Exception {
			UserSearchRequestDTO req = buildSearchRequest(null, null, null, null, null, null);
			Page<UserResponseDTO> page = new PageImpl<>(
					List.of(userResponse),
					PageRequest.of(1, 5),
					6L
			);
			when(userService.searchUsers(any(), any())).thenReturn(page);

			mockMvc.perform(post(SEARCH_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(req))
							.param("page", "1")
							.param("size", "5"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.totalElements", is(6)))
					.andExpect(jsonPath("$.size",          is(5)));
		}

		@Test
		@Order(7)
		@WithMockUser
		@DisplayName("404 NOT FOUND - service throws ResourceNotFoundException")
		void search_serviceThrowsNotFound_returns404() throws Exception {
			UserSearchRequestDTO req = buildSearchRequest(null, null, null, null, null, null);
			when(userService.searchUsers(any(), any()))
					.thenThrow(new ResourceNotFoundException("No users found"));

			mockMvc.perform(post(SEARCH_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(req)))
					.andExpect(status().isNotFound());
		}

		@Test
		@Order(8)
		@WithMockUser
		@DisplayName("200 OK - multiple users returned on single page")
		void search_multipleUsers_allReturnedInContent() throws Exception {
			UserSearchRequestDTO req = buildSearchRequest(null, null, null, null, null, null);
			List<UserResponseDTO> users = List.of(
					buildUserResponse(1L, "user1@test.com", Role.ROLE_USER),
					buildUserResponse(2L, "user2@test.com", Role.ROLE_ADMIN),
					buildUserResponse(3L, "user3@test.com", Role.ROLE_USER)
			);
			when(userService.searchUsers(any(), any())).thenReturn(new PageImpl<>(users));

			mockMvc.perform(post(SEARCH_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(req)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content", hasSize(3)))
					.andExpect(jsonPath("$.content[1].email", is("user2@test.com")));
		}
	}


	@Nested
	@DisplayName("Update User - PATCH /updateUser/{id}")
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class UpdateUserTests {

		@Test
		@Order(1)
		@WithMockUser
		@DisplayName("200 OK - update email and role successfully")
		void updateUser_validRequest_returns200() throws Exception {
			UserUpdateRequestDTO req = new UserUpdateRequestDTO("updated@example.com", Role.ROLE_ADMIN);
			UserResponseDTO updated   = buildUserResponse(USER_ID, "updated@example.com", Role.ROLE_ADMIN);
			when(userService.updateUser(eq(USER_ID), any())).thenReturn(updated);

			mockMvc.perform(patch(UPDATE_URL, USER_ID)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(req)))
					.andDo(print())
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.id",    is((int) USER_ID)))
					.andExpect(jsonPath("$.email", is("updated@example.com")))
					.andExpect(jsonPath("$.role",  is("ROLE_ADMIN")));

			verify(userService, times(1)).updateUser(eq(USER_ID), any());
		}

		@Test
		@Order(2)
		@WithMockUser
		@DisplayName("200 OK - update email only (role unchanged)")
		void updateUser_emailOnly_returns200() throws Exception {
			UserUpdateRequestDTO req = new UserUpdateRequestDTO("newemail@example.com", null);
			UserResponseDTO updated   = buildUserResponse(USER_ID, "newemail@example.com", Role.ROLE_USER);
			when(userService.updateUser(eq(USER_ID), any())).thenReturn(updated);

			mockMvc.perform(patch(UPDATE_URL, USER_ID)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(req)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.email", is("newemail@example.com")));
		}

		@Test
		@Order(3)
		@WithMockUser
		@DisplayName("200 OK - update role only (email unchanged)")
		void updateUser_roleOnly_returns200() throws Exception {
			UserUpdateRequestDTO req = new UserUpdateRequestDTO(null, Role.ROLE_ADMIN);
			UserResponseDTO updated   = buildUserResponse(USER_ID, USER_EMAIL, Role.ROLE_ADMIN);
			when(userService.updateUser(eq(USER_ID), any())).thenReturn(updated);

			mockMvc.perform(patch(UPDATE_URL, USER_ID)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(req)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.role", is("ROLE_ADMIN")));
		}

		@Test
		@Order(4)
		@WithMockUser
		@DisplayName("404 NOT FOUND - update non-existent user")
		void updateUser_notFound_returns404() throws Exception {
			UserUpdateRequestDTO req = new UserUpdateRequestDTO("x@x.com", Role.ROLE_USER);
			when(userService.updateUser(eq(99L), any()))
					.thenThrow(new ResourceNotFoundException("User not found with id: 99"));

			mockMvc.perform(patch(UPDATE_URL, 99L)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(req)))
					.andDo(print())
					.andExpect(status().isNotFound());
		}

//		@Test
//		@Order(5)
//		@WithMockUser
//		@DisplayName("400 BAD REQUEST - invalid email format in update")
//		void updateUser_invalidEmail_returns400() throws Exception {
//			UserUpdateRequestDTO req = new UserUpdateRequestDTO("bad-email", Role.ROLE_USER);
//
//			mockMvc.perform(patch(UPDATE_URL, USER_ID)
//							.contentType(MediaType.APPLICATION_JSON)
//							.content(toJson(req)))
//					.andExpect(status().isBadRequest());
//
//			verify(userService, never()).updateUser(anyLong(), any());
//		}

		@Test
		@Order(6)
		@WithMockUser
		@DisplayName("200 OK - update different user IDs (ADMIN_ID)")
		void updateUser_differentUserId_returns200() throws Exception {
			UserUpdateRequestDTO req  = new UserUpdateRequestDTO(ADMIN_EMAIL, Role.ROLE_ADMIN);
			UserResponseDTO      resp = buildUserResponse(ADMIN_ID, ADMIN_EMAIL, Role.ROLE_ADMIN);
			when(userService.updateUser(eq(ADMIN_ID), any())).thenReturn(resp);

			mockMvc.perform(patch(UPDATE_URL, ADMIN_ID)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(req)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.id", is((int) ADMIN_ID)));
		}
	}



	@Nested
	@DisplayName("Get User By ID - GET /getById/{id}")
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class GetUserByIdTests {

		@Test
		@Order(1)
		@WithMockUser
		@DisplayName("200 OK - returns full UserResponseDTO for existing user")
		void getUserById_exists_returns200() throws Exception {
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

		@Test
		@Order(2)
		@WithMockUser
		@DisplayName("200 OK - returns admin user details correctly")
		void getUserById_adminUser_returns200() throws Exception {
			when(userService.getUserById(ADMIN_ID)).thenReturn(adminResponse);

			mockMvc.perform(get(GET_BY_ID_URL, ADMIN_ID))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.id",    is((int) ADMIN_ID)))
					.andExpect(jsonPath("$.email", is(ADMIN_EMAIL)))
					.andExpect(jsonPath("$.role",  is("ROLE_ADMIN")));
		}

		@Test
		@Order(3)
		@WithMockUser
		@DisplayName("404 NOT FOUND - user with given ID does not exist")
		void getUserById_notFound_returns404() throws Exception {
			when(userService.getUserById(999L))
					.thenThrow(new ResourceNotFoundException("User not found with id: 999"));

			mockMvc.perform(get(GET_BY_ID_URL, 999L))
					.andDo(print())
					.andExpect(status().isNotFound());
		}

		@Test
		@Order(4)
		@WithMockUser
		@DisplayName("Response includes createdAt and updatedAt timestamps")
		void getUserById_responseContainsTimestamps() throws Exception {
			when(userService.getUserById(USER_ID)).thenReturn(userResponse);

			mockMvc.perform(get(GET_BY_ID_URL, USER_ID))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.createdAt").exists())
					.andExpect(jsonPath("$.updatedAt").exists());
		}

		@Test
		@Order(5)
		@WithMockUser
		@DisplayName("Service called exactly once with correct ID")
		void getUserById_serviceCalledOnceWithCorrectId() throws Exception {
			when(userService.getUserById(USER_ID)).thenReturn(userResponse);

			mockMvc.perform(get(GET_BY_ID_URL, USER_ID))
					.andExpect(status().isOk());

			verify(userService, times(1)).getUserById(USER_ID);
			verifyNoMoreInteractions(userService);
		}
	}


	@Nested
	@DisplayName("Soft Delete User - DELETE /deleteUser/{id}")
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class SoftDeleteUserTests {

		@Test
		@Order(1)
		@WithMockUser
		@DisplayName("200 OK - soft delete returns full DeleteResponseDTO")
		void softDelete_validId_returns200() throws Exception {
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

		@Test
		@Order(2)
		@WithMockUser
		@DisplayName("200 OK - response contains audit fields after deletion")
		void softDelete_responseContainsAuditFields() throws Exception {
			when(userService.softDeleteUser(USER_ID)).thenReturn(deleteResponse);

			mockMvc.perform(delete(DELETE_URL, USER_ID))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.createdById", is((int) ADMIN_ID)))
					.andExpect(jsonPath("$.updatedById", is((int) ADMIN_ID)));
		}

		@Test
		@Order(3)
		@WithMockUser
		@DisplayName("404 NOT FOUND - delete non-existent user")
		void softDelete_notFound_returns404() throws Exception {
			when(userService.softDeleteUser(999L))
					.thenThrow(new ResourceNotFoundException("User not found with id: 999"));

			mockMvc.perform(delete(DELETE_URL, 999L))
					.andDo(print())
					.andExpect(status().isNotFound());
		}

		@Test
		@Order(4)
		@WithMockUser
		@DisplayName("200 OK - delete admin user returns correct role")
		void softDelete_adminUser_returns200WithAdminRole() throws Exception {
			DeleteResponseDTO adminDelete = new DeleteResponseDTO(
					ADMIN_ID, ADMIN_EMAIL, Role.ROLE_ADMIN,
					CREATED_AT, UPDATED_AT, ADMIN_ID, ADMIN_ID,
					true, "User soft-deleted successfully"
			);
			when(userService.softDeleteUser(ADMIN_ID)).thenReturn(adminDelete);

			mockMvc.perform(delete(DELETE_URL, ADMIN_ID))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.role",      is("ROLE_ADMIN")))
					.andExpect(jsonPath("$.isDeleted", is(true)));
		}

		@Test
		@Order(5)
		@WithMockUser
		@DisplayName("Service called exactly once with correct ID")
		void softDelete_serviceCalledOnce() throws Exception {
			when(userService.softDeleteUser(USER_ID)).thenReturn(deleteResponse);

			mockMvc.perform(delete(DELETE_URL, USER_ID))
					.andExpect(status().isOk());

			verify(userService, times(1)).softDeleteUser(USER_ID);
			verifyNoMoreInteractions(userService);
		}
	}


	@Nested
	@DisplayName("Change Password - PATCH /changePassword")
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class ChangePasswordTests {

		@Test
		@Order(1)
		@WithMockUser
		@DisplayName("200 OK - valid old and new password returns success response")
		void changePassword_valid_returns200() throws Exception {
			ChangePasswordRequestDTO req = new ChangePasswordRequestDTO(VALID_PASSWORD, NEW_PASSWORD);
			when(userService.changePassword(any())).thenReturn(changePassResponse);

			mockMvc.perform(patch(CHANGE_PASS_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(req)))
					.andDo(print())
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.userId",  is((int) USER_ID)))
					.andExpect(jsonPath("$.email",   is(USER_EMAIL)))
					.andExpect(jsonPath("$.message", is("Password changed successfully")));

			verify(userService, times(1)).changePassword(any(ChangePasswordRequestDTO.class));
		}

		@Test
		@Order(2)
		@WithMockUser
		@DisplayName("400 BAD REQUEST - old password is blank")
		void changePassword_blankOldPassword_returns400() throws Exception {
			ChangePasswordRequestDTO req = new ChangePasswordRequestDTO("", NEW_PASSWORD);

			mockMvc.perform(patch(CHANGE_PASS_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(req)))
					.andDo(print())
					.andExpect(status().isBadRequest());

			verify(userService, never()).changePassword(any());
		}

		@Test
		@Order(3)
		@WithMockUser
		@DisplayName("400 BAD REQUEST - new password is blank")
		void changePassword_blankNewPassword_returns400() throws Exception {
			ChangePasswordRequestDTO req = new ChangePasswordRequestDTO(VALID_PASSWORD, "");

			mockMvc.perform(patch(CHANGE_PASS_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(req)))
					.andDo(print())
					.andExpect(status().isBadRequest());

			verify(userService, never()).changePassword(any());
		}

		@Test
		@Order(4)
		@WithMockUser
		@DisplayName("400 BAD REQUEST - null old password")
		void changePassword_nullOldPassword_returns400() throws Exception {
			ChangePasswordRequestDTO req = new ChangePasswordRequestDTO(null, NEW_PASSWORD);

			mockMvc.perform(patch(CHANGE_PASS_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(req)))
					.andExpect(status().isBadRequest());

			verify(userService, never()).changePassword(any());
		}

		@Test
		@Order(5)
		@WithMockUser
		@DisplayName("400 BAD REQUEST - null new password")
		void changePassword_nullNewPassword_returns400() throws Exception {
			ChangePasswordRequestDTO req = new ChangePasswordRequestDTO(VALID_PASSWORD, null);

			mockMvc.perform(patch(CHANGE_PASS_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(req)))
					.andExpect(status().isBadRequest());

			verify(userService, never()).changePassword(any());
		}

		@Test
		@Order(6)
		@WithMockUser
		@DisplayName("400 BAD REQUEST - empty JSON body")
		void changePassword_emptyBody_returns400() throws Exception {
			mockMvc.perform(patch(CHANGE_PASS_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{}"))
					.andExpect(status().isBadRequest());
		}

		@Test
		@Order(7)
		@WithMockUser
		@DisplayName("401 UNAUTHORIZED - wrong old password throws BadCredentialsException")
		void changePassword_wrongOldPassword_returns401() throws Exception {
			ChangePasswordRequestDTO req = new ChangePasswordRequestDTO("wrongOld", NEW_PASSWORD);
			when(userService.changePassword(any()))
					.thenThrow(new BadCredentialsException("Incorrect old password"));

			mockMvc.perform(patch(CHANGE_PASS_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(req)))
					.andExpect(status().isUnauthorized());
		}

		@Test
		@Order(8)
		@WithMockUser
		@DisplayName("404 NOT FOUND - user not found during password change")
		void changePassword_userNotFound_returns404() throws Exception {
			ChangePasswordRequestDTO req = new ChangePasswordRequestDTO(VALID_PASSWORD, NEW_PASSWORD);
			when(userService.changePassword(any()))
					.thenThrow(new ResourceNotFoundException("User not found"));

			mockMvc.perform(patch(CHANGE_PASS_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(req)))
					.andExpect(status().isNotFound());
		}

		@Test
		@Order(9)
		@WithMockUser
		@DisplayName("Service called exactly once on successful change")
		void changePassword_serviceCalledOnce() throws Exception {
			ChangePasswordRequestDTO req = new ChangePasswordRequestDTO(VALID_PASSWORD, NEW_PASSWORD);
			when(userService.changePassword(any())).thenReturn(changePassResponse);

			mockMvc.perform(patch(CHANGE_PASS_URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content(toJson(req)))
					.andExpect(status().isOk());

			verify(userService, times(1)).changePassword(any());
			verifyNoMoreInteractions(userService);
		}
	}
}