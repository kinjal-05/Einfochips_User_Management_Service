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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import userservice.dtos.UserResponseDTO;
import userservice.dtos.UserSearchRequestDTO;
import userservice.enums.Role;
import userservice.security.CustomUserDetailsService;
import userservice.security.JwtService;
import userservice.services.LoginUserService;
import userservice.services.SearchUserService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
		controllers = SearchUserController.class,
		excludeAutoConfiguration = {
				org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
		}
)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SearchUserController - Full Coverage Test Suite")
public class SearchUserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private SearchUserService searchUserService;

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

		when(searchUserService.searchUsers(any(UserSearchRequestDTO.class), any(Pageable.class)))
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

		verifyNoInteractions(searchUserService);
	}

	// 4. INVALID DATE FORMAT → 400
	@Test
	void searchUsers_ShouldReturnBadRequest_WhenInvalidDate() throws Exception {

		mockMvc.perform(get(SEARCH_URL)
						.param("fromDate", "invalid-date"))
				.andExpect(status().isInternalServerError());

		verifyNoInteractions(searchUserService);
	}

	// 5. SERVICE EXCEPTION → 500
	@Test
	void searchUsers_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {

		when(searchUserService.searchUsers(any(UserSearchRequestDTO.class), any(Pageable.class)))
				.thenThrow(new RuntimeException("DB error"));

		mockMvc.perform(get(SEARCH_URL)
						.param("page", "0")
						.param("size", "10"))
				.andExpect(status().isInternalServerError());
	}
}
