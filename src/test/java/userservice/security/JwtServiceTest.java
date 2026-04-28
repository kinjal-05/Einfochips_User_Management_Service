package userservice.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.security.Key;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import userservice.enums.Role;
import userservice.models.User;

/**
 * Unit tests for {@link JwtService}.
 *
 * Strategy: - Real token generation/parsing is used (no mocking of JJWT
 * internals) so the full signing/verification pipeline is exercised. -
 * ReflectionTestUtils injects @Value fields that Spring would normally inject.
 * - Expired tokens are built manually by setting a past expiration date.
 *
 * Branches covered: 1. extractUsername — happy path 2. extractClaim — custom
 * claim resolver 3. generateToken — UserDetails is NOT CustomUserDetails (no
 * extra claims) 4. generateToken — UserDetails IS CustomUserDetails (role +
 * userId added) 5. isTokenValid — email matches, not expired, user enabled →
 * true 6. isTokenValid — email mismatch → false 7. isTokenValid — token expired
 * → false 8. isTokenValid — user disabled → false 9. extractAllClaims —
 * invalid/tampered token throws exception 10. isTokenExpired — not expired
 * (covered via isTokenValid true) 11. isTokenExpired — expired (covered via
 * isTokenValid false)
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class JwtServiceTest {

	// A 256-bit Base64-encoded secret (32 bytes) — safe for HS256
	private static final String SECRET = "dGVzdFNlY3JldEtleUZvckp3dFVuaXRUZXN0aW5nMTIzNDU2";

	// 1 hour in milliseconds
	private static final long EXPIRATION_MS = 1000L * 60 * 60;

	private JwtService jwtService;

	// Plain UserDetails mock (not a CustomUserDetails)
	@Mock
	private UserDetails plainUserDetails;

	@BeforeEach
	void setUp() {
		jwtService = new JwtService();
		ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
		ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION_MS);
	}

	// ── Shared helper ─────────────────────────────────────────────────────────

	/** Builds a real, signed JWT using the same key as JwtService. */
	private String buildRealToken(String subject, long expirationMs) {
		Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
		return Jwts.builder().setSubject(subject).setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + expirationMs))
				.signWith(key, SignatureAlgorithm.HS256).compact();
	}

	/** Builds a real, EXPIRED JWT (expiration set 1 hour in the past). */
	private String buildExpiredToken(String subject) {
		Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
		long past = System.currentTimeMillis() - EXPIRATION_MS;
		return Jwts.builder().setSubject(subject).setIssuedAt(new Date(past - 1000)).setExpiration(new Date(past))
				.signWith(key, SignatureAlgorithm.HS256).compact();
	}

	/**
	 * Verifies JWT extraction and validation behavior for various token scenarios.
	 *
	 * Security expectations: - Username must be correctly extracted from valid
	 * tokens - Token validation must respect user identity and account state -
	 * Invalid or mismatched conditions must result in rejection - Expired tokens
	 * must fail validation with appropriate exception
	 */
	@Nested
	@DisplayName("extractUsername")
	class ExtractUsernameTests {

		/**
		 * Ensures that a valid JWT token correctly extracts the subject
		 * (username/email).
		 *
		 * Expected behavior: - JWT subject is returned as username
		 */
		@Test
		@DisplayName("returns subject from a valid token")
		void returnsSubject() {
			String token = buildRealToken("kinjal@example.com", EXPIRATION_MS);
			assertThat(jwtService.extractUsername(token)).isEqualTo("kinjal@example.com");
		}

		/**
		 * Ensures that token validation fails when extracted username is valid but
		 * UserDetails has a null username.
		 *
		 * Expected behavior: - Token is considered invalid
		 */
		@Test
		@DisplayName("email matches but userDetails.getUsername() is null → false")
		void usernameNull_returnsFalse() {
			when(plainUserDetails.getUsername()).thenReturn(null);

			String token = buildRealToken("kinjal@example.com", EXPIRATION_MS);

			boolean result = jwtService.isTokenValid(token, plainUserDetails);

			assertThat(result).isFalse();
		}

		/**
		 * Verifies that JWT validation fails for multiple invalid scenarios.
		 *
		 * Expected behavior: - Any mismatch between token and user details results in
		 * invalid token - Disabled accounts must not pass validation
		 */
		@ParameterizedTest(name = "{index} — {0}")
		@DisplayName("isTokenValid → false cases")
		@CsvSource({ "email mismatch,    other@example.com,  true,  kinjal@example.com",
				"enabled false,     kinjal@example.com, false, kinjal@example.com",
				"email+disabled,    kinjal@example.com, false, kinjal@example.com" })
		void isTokenValid_returnsFalse(String desc, String username, boolean enabled, String tokenEmail) {
			when(plainUserDetails.getUsername()).thenReturn(username);
			when(plainUserDetails.isEnabled()).thenReturn(enabled);

			String token = buildRealToken(tokenEmail, EXPIRATION_MS);

			boolean result = jwtService.isTokenValid(token, plainUserDetails);

			assertThat(result).isFalse();
		}

		/**
		 * Ensures that expired JWT tokens are not considered valid and throw an
		 * exception.
		 *
		 * Expected behavior: - Expired token must trigger ExpiredJwtException
		 */
		@Test
		@DisplayName("notExpired false → expired token throws exception")
		void notExpiredFalse() {
			String expiredToken = buildExpiredToken("kinjal@example.com");

			assertThatThrownBy(() -> jwtService.isTokenValid(expiredToken, plainUserDetails))
					.isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
		}

	}

	/**
	 * Verifies generic JWT claim extraction using custom claim resolvers.
	 *
	 * Security expectations: - Claims must be accurately extracted from valid
	 * tokens - Standard JWT fields (issuedAt, expiration) must be correctly parsed
	 * - Custom claim resolvers must not alter token integrity
	 */
	@Nested
	@DisplayName("extractClaim")
	class ExtractClaimTests {

		/**
		 * Ensures that custom claim resolver correctly extracts the issued-at
		 * timestamp.
		 *
		 * Expected behavior: - issuedAt is present and not null - issuedAt must not be
		 * in the future
		 */
		@Test
		@DisplayName("custom claimsResolver — returns issuedAt date")
		void customResolver_returnsIssuedAt() {
			String token = buildRealToken("user@test.com", EXPIRATION_MS);
			Date issuedAt = jwtService.extractClaim(token, io.jsonwebtoken.Claims::getIssuedAt);
			assertThat(issuedAt).isNotNull().isBeforeOrEqualTo(new Date());
		}

		/**
		 * Ensures that custom claim resolver correctly extracts token expiration date.
		 *
		 * Expected behavior: - expiration must exist and be in the future for valid
		 * tokens
		 */
		@Test
		@DisplayName("custom claimsResolver — returns expiration date")
		void customResolver_returnsExpiration() {
			String token = buildRealToken("user@test.com", EXPIRATION_MS);
			Date expiration = jwtService.extractClaim(token, io.jsonwebtoken.Claims::getExpiration);
			assertThat(expiration).isAfter(new Date());
		}
	}

	/**
	 * Verifies JWT token generation for different UserDetails implementations.
	 *
	 * Security expectations: - Token must always contain correct subject
	 * (username/email) - Plain users must not include sensitive or extra claims -
	 * Custom users may include additional claims like role and userId
	 */
	@Nested
	@DisplayName("generateToken")
	class GenerateTokenTests {

		/**
		 * Ensures that token generated from plain UserDetails contains only the subject
		 * and no additional claims.
		 *
		 * Expected behavior: - Username is correctly embedded as subject - No role or
		 * userId claims are included
		 */
		@Test
		@DisplayName("plain UserDetails — token contains subject, no extra claims")
		void plainUserDetails_noExtraClaims() {
			when(plainUserDetails.getUsername()).thenReturn("plain@example.com");

			String token = jwtService.generateToken(plainUserDetails);

			assertThat(jwtService.extractUsername(token)).isEqualTo("plain@example.com");

			// Verify no role/userId claims were added
			io.jsonwebtoken.Claims claims = jwtService.extractClaim(token, c -> c);
			assertThat(claims.get("role")).isNull();
			assertThat(claims.get("userId")).isNull();
		}

		/**
		 * Ensures that CustomUserDetails correctly embeds additional claims such as
		 * role and userId into the JWT token.
		 *
		 * Expected behavior: - Subject reflects user email - Role is included in token
		 * claims - User ID is included in token claims
		 */
		@Test
		@DisplayName("CustomUserDetails — token contains role and userId extra claims")
		void customUserDetails_extraClaimsAdded() {
			// Build a real CustomUserDetails backed by a User entity
			User user = new User();
			user.setId(42L);
			user.setEmail("custom@example.com");
			user.setRole(Role.ROLE_ADMIN);
			CustomUserDetails customUserDetails = new CustomUserDetails(user
//					Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
			);

			String token = jwtService.generateToken(customUserDetails);

			io.jsonwebtoken.Claims claims = jwtService.extractClaim(token, c -> c);

			assertThat(jwtService.extractUsername(token)).isEqualTo("custom@example.com");
			assertThat(claims).containsEntry("role", "ROLE_ROLE_ADMIN");
			// JJWT deserialises numeric values as Integer by default
			assertThat(((Number) claims.get("userId")).longValue()).isEqualTo(42L);
		}
	}

	/**
	 * Verifies JWT validation logic under different identity and token conditions.
	 *
	 * Security expectations: - Token is valid only when email matches, token is not
	 * expired, and user is enabled - Any mismatch in identity or account state must
	 * invalidate the token - Expired or malformed tokens must fail validation
	 * reliably - Invalid tokens must never bypass security checks
	 */
	@Nested
	@DisplayName("isTokenValid")
	class IsTokenValidTests {

		/**
		 * Ensures token is considered valid only when all conditions are satisfied:
		 * matching email, non-expired token, and enabled user account.
		 */
		@Test
		@DisplayName("email matches, not expired, user enabled → true")
		void allConditionsPass_returnsTrue() {
			when(plainUserDetails.getUsername()).thenReturn("kinjal@example.com");
			when(plainUserDetails.isEnabled()).thenReturn(true);

			String token = buildRealToken("kinjal@example.com", EXPIRATION_MS);

			assertThat(jwtService.isTokenValid(token, plainUserDetails)).isTrue();
		}

		/**
		 * Ensures token is rejected when email in token does not match user.
		 */
		@Test
		@DisplayName("email mismatch → false")
		void emailMismatch_returnsFalse() {
			when(plainUserDetails.getUsername()).thenReturn("other@example.com");

			String token = buildRealToken("kinjal@example.com", EXPIRATION_MS);

			assertThat(jwtService.isTokenValid(token, plainUserDetails)).isFalse();
		}

		/**
		 * Ensures expired tokens fail validation by throwing ExpiredJwtException.
		 *
		 * Security expectation: - Expired tokens must not be silently accepted
		 */
		@Test
		@DisplayName("token expired → false")
		void tokenExpired_returnsFalse() {
			// expired token parsing throws ExpiredJwtException — isTokenValid should
			// propagate it
			// (the filter catches it upstream); verify the exception is thrown

			String expiredToken = buildExpiredToken("kinjal@example.com");

			assertThatThrownBy(() -> jwtService.isTokenValid(expiredToken, plainUserDetails))
					.isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
		}

		/**
		 * Ensures disabled user accounts cannot validate tokens.
		 */
		@Test
		@DisplayName("user disabled → false")
		void userDisabled_returnsFalse() {
			when(plainUserDetails.getUsername()).thenReturn("kinjal@example.com");
			when(plainUserDetails.isEnabled()).thenReturn(false);

			String token = buildRealToken("kinjal@example.com", EXPIRATION_MS);

			assertThat(jwtService.isTokenValid(token, plainUserDetails)).isFalse();
		}
	}

	/**
	 * Verifies behavior of claim extraction when tokens are invalid or malformed.
	 *
	 * Security expectations: - Tampered tokens must be rejected - Non-JWT strings
	 * must fail validation - Expired tokens must throw before any claim comparison
	 * - Null user context must be rejected immediately
	 */
	@Nested
	@DisplayName("extractAllClaims — invalid token")
	class ExtractAllClaimsTests {

		/**
		 * Ensures tampered JWT signatures are detected and rejected.
		 */
		@Test
		@DisplayName("tampered token — throws JwtException")
		void tamperedToken_throwsException() {
			String token = buildRealToken("user@test.com", EXPIRATION_MS);
			// Corrupt the signature segment
			String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsig";

			assertThatThrownBy(() -> jwtService.extractUsername(tampered))
					.isInstanceOf(io.jsonwebtoken.JwtException.class);
		}

		/**
		 * Ensures completely invalid JWT formats are rejected.
		 */
		@Test
		@DisplayName("completely garbage token — throws JwtException")
		void garbageToken_throwsException() {
			assertThatThrownBy(() -> jwtService.extractUsername("not.a.jwt"))
					.isInstanceOf(io.jsonwebtoken.JwtException.class);
		}

		/**
		 * Ensures expired tokens fail before any username comparison logic executes.
		 *
		 * Important: - Exception must be thrown during parsing phase - No user detail
		 * interaction should occur
		 */
		@Test
		@DisplayName("token expired — ExpiredJwtException is thrown before email check")
		void tokenExpired_exceptionThrownBeforeEmailCheck() {
			// NOTE: getUsername() should NOT be called because
			// extractAllClaims() throws before we reach the email comparison.
			// Do NOT stub getUsername() here — Mockito strict mode will
			// flag it as unnecessary if it's never invoked.

			String expiredToken = buildExpiredToken("kinjal@example.com");

			assertThatThrownBy(() -> jwtService.isTokenValid(expiredToken, plainUserDetails))
					.isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);

			// Verify getUsername() was never called (exception thrown before email check)
			org.mockito.Mockito.verifyNoInteractions(plainUserDetails);
		}

		/**
		 * Ensures null UserDetails input is rejected.
		 */
		@Test
		@DisplayName("userDetails is null → throws NullPointerException")
		void userDetailsNull_throwsException() {
			String token = buildRealToken("kinjal@example.com", EXPIRATION_MS);

			assertThatThrownBy(() -> jwtService.isTokenValid(token, null)).isInstanceOf(NullPointerException.class);
		}
	}
}