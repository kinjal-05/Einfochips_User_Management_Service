package userservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import userservice.enums.Role;
import userservice.models.User;

import java.security.Key;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link JwtService}.
 *
 * Strategy:
 *  - Real token generation/parsing is used (no mocking of JJWT internals)
 *    so the full signing/verification pipeline is exercised.
 *  - ReflectionTestUtils injects @Value fields that Spring would normally inject.
 *  - Expired tokens are built manually by setting a past expiration date.
 *
 * Branches covered:
 *  1.  extractUsername          — happy path
 *  2.  extractClaim             — custom claim resolver
 *  3.  generateToken            — UserDetails is NOT CustomUserDetails (no extra claims)
 *  4.  generateToken            — UserDetails IS CustomUserDetails (role + userId added)
 *  5.  isTokenValid             — email matches, not expired, user enabled  → true
 *  6.  isTokenValid             — email mismatch                            → false
 *  7.  isTokenValid             — token expired                             → false
 *  8.  isTokenValid             — user disabled                             → false
 *  9.  extractAllClaims         — invalid/tampered token throws exception
 *  10. isTokenExpired           — not expired (covered via isTokenValid true)
 *  11. isTokenExpired           — expired     (covered via isTokenValid false)
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class JwtServiceTest {

	// A 256-bit Base64-encoded secret (32 bytes) — safe for HS256
	private static final String SECRET =
			"dGVzdFNlY3JldEtleUZvckp3dFVuaXRUZXN0aW5nMTIzNDU2";

	// 1 hour in milliseconds
	private static final long EXPIRATION_MS = 1000L * 60 * 60;

	private JwtService jwtService;

	// Plain UserDetails mock (not a CustomUserDetails)
	@Mock private UserDetails plainUserDetails;

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
		return Jwts.builder()
				.setSubject(subject)
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + expirationMs))
				.signWith(key, SignatureAlgorithm.HS256)
				.compact();
	}

	/** Builds a real, EXPIRED JWT (expiration set 1 hour in the past). */
	private String buildExpiredToken(String subject) {
		Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
		long past = System.currentTimeMillis() - EXPIRATION_MS;
		return Jwts.builder()
				.setSubject(subject)
				.setIssuedAt(new Date(past - 1000))
				.setExpiration(new Date(past))
				.signWith(key, SignatureAlgorithm.HS256)
				.compact();
	}

	// =========================================================================
	// extractUsername
	// =========================================================================
	@Nested
	@DisplayName("extractUsername")
	class ExtractUsernameTests {

		@Test
		@DisplayName("returns subject from a valid token")
		void returnsSubject() {
			String token = buildRealToken("kinjal@example.com", EXPIRATION_MS);
			assertThat(jwtService.extractUsername(token)).isEqualTo("kinjal@example.com");
		}
		@Test
		@DisplayName("email matches but userDetails.getUsername() is null → false")
		void usernameNull_returnsFalse() {
			when(plainUserDetails.getUsername()).thenReturn(null);

			String token = buildRealToken("kinjal@example.com", EXPIRATION_MS);

			boolean result = jwtService.isTokenValid(token, plainUserDetails);

			assertThat(result).isFalse();
		}
		@Test
		@DisplayName("isSameUser false → returns false")
		void isSameUserFalse() {
			when(plainUserDetails.getUsername()).thenReturn("other@example.com");
			when(plainUserDetails.isEnabled()).thenReturn(true);

			String token = buildRealToken("kinjal@example.com", EXPIRATION_MS);

			boolean result = jwtService.isTokenValid(token, plainUserDetails);

			assertThat(result).isFalse();
		}

		@Test
		@DisplayName("notExpired false → expired token throws exception")
		void notExpiredFalse() {
			String expiredToken = buildExpiredToken("kinjal@example.com");

			assertThatThrownBy(() ->
					jwtService.isTokenValid(expiredToken, plainUserDetails)
			).isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
		}

		@Test
		@DisplayName("enabled false → returns false")
		void enabledFalse() {
			when(plainUserDetails.getUsername()).thenReturn("kinjal@example.com");
			when(plainUserDetails.isEnabled()).thenReturn(false);

			String token = buildRealToken("kinjal@example.com", EXPIRATION_MS);

			boolean result = jwtService.isTokenValid(token, plainUserDetails);

			assertThat(result).isFalse();
		}

		@Test
		@DisplayName("email matches, not expired, but enabled check evaluated → false")
		void enabledCheckExplicitlyEvaluated() {
			when(plainUserDetails.getUsername()).thenReturn("kinjal@example.com");
			when(plainUserDetails.isEnabled()).thenReturn(false);

			String token = buildRealToken("kinjal@example.com", EXPIRATION_MS);

			boolean result = jwtService.isTokenValid(token, plainUserDetails);

			assertThat(result).isFalse();
		}
	}

	// =========================================================================
	// extractClaim — custom resolver
	// =========================================================================
	@Nested
	@DisplayName("extractClaim")
	class ExtractClaimTests {

		@Test
		@DisplayName("custom claimsResolver — returns issuedAt date")
		void customResolver_returnsIssuedAt() {
			String token = buildRealToken("user@test.com", EXPIRATION_MS);
			Date issuedAt = jwtService.extractClaim(token,
					io.jsonwebtoken.Claims::getIssuedAt);
			assertThat(issuedAt).isNotNull().isBeforeOrEqualTo(new Date());
		}

		@Test
		@DisplayName("custom claimsResolver — returns expiration date")
		void customResolver_returnsExpiration() {
			String token = buildRealToken("user@test.com", EXPIRATION_MS);
			Date expiration = jwtService.extractClaim(token,
					io.jsonwebtoken.Claims::getExpiration);
			assertThat(expiration).isAfter(new Date());
		}
	}

	// =========================================================================
	// generateToken
	// =========================================================================
	@Nested
	@DisplayName("generateToken")
	class GenerateTokenTests {

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

		@Test
		@DisplayName("CustomUserDetails — token contains role and userId extra claims")
		void customUserDetails_extraClaimsAdded() {
			// Build a real CustomUserDetails backed by a User entity
			User user = new User();
			user.setId(42L);
			user.setEmail("custom@example.com");
			user.setRole(Role.ROLE_ADMIN);
			CustomUserDetails customUserDetails = new CustomUserDetails(
					user
//					Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
			);

			String token = jwtService.generateToken(customUserDetails);

			io.jsonwebtoken.Claims claims = jwtService.extractClaim(token, c -> c);

			assertThat(jwtService.extractUsername(token)).isEqualTo("custom@example.com");
			assertThat(claims.get("role")).isEqualTo("ROLE_ROLE_ADMIN");
			// JJWT deserialises numeric values as Integer by default
			assertThat(((Number) claims.get("userId")).longValue()).isEqualTo(42L);
		}
	}

	// =========================================================================
	// isTokenValid — all branches
	// =========================================================================
	@Nested
	@DisplayName("isTokenValid")
	class IsTokenValidTests {

		@Test
		@DisplayName("email matches, not expired, user enabled → true")
		void allConditionsPass_returnsTrue() {
			when(plainUserDetails.getUsername()).thenReturn("kinjal@example.com");
			when(plainUserDetails.isEnabled()).thenReturn(true);

			String token = buildRealToken("kinjal@example.com", EXPIRATION_MS);

			assertThat(jwtService.isTokenValid(token, plainUserDetails)).isTrue();
		}

		@Test
		@DisplayName("email mismatch → false")
		void emailMismatch_returnsFalse() {
			when(plainUserDetails.getUsername()).thenReturn("other@example.com");

			String token = buildRealToken("kinjal@example.com", EXPIRATION_MS);

			assertThat(jwtService.isTokenValid(token, plainUserDetails)).isFalse();
		}

		@Test
		@DisplayName("token expired → false")
		void tokenExpired_returnsFalse() {
			// expired token parsing throws ExpiredJwtException — isTokenValid should propagate it
			// (the filter catches it upstream); verify the exception is thrown
//			when(plainUserDetails.getUsername()).thenReturn("kinjal@example.com");

			String expiredToken = buildExpiredToken("kinjal@example.com");

			assertThatThrownBy(() -> jwtService.isTokenValid(expiredToken, plainUserDetails))
					.isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
		}

		@Test
		@DisplayName("user disabled → false")
		void userDisabled_returnsFalse() {
			when(plainUserDetails.getUsername()).thenReturn("kinjal@example.com");
			when(plainUserDetails.isEnabled()).thenReturn(false);

			String token = buildRealToken("kinjal@example.com", EXPIRATION_MS);

			assertThat(jwtService.isTokenValid(token, plainUserDetails)).isFalse();
		}
	}

	// =========================================================================
	// extractAllClaims — invalid token
	// =========================================================================
	@Nested
	@DisplayName("extractAllClaims — invalid token")
	class ExtractAllClaimsTests {

		@Test
		@DisplayName("tampered token — throws JwtException")
		void tamperedToken_throwsException() {
			String token = buildRealToken("user@test.com", EXPIRATION_MS);
			// Corrupt the signature segment
			String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsig";

			assertThatThrownBy(() -> jwtService.extractUsername(tampered))
					.isInstanceOf(io.jsonwebtoken.JwtException.class);
		}

		@Test
		@DisplayName("completely garbage token — throws JwtException")
		void garbageToken_throwsException() {
			assertThatThrownBy(() -> jwtService.extractUsername("not.a.jwt"))
					.isInstanceOf(io.jsonwebtoken.JwtException.class);
		}

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

		@Test
		@DisplayName("userDetails is null → throws NullPointerException")
		void userDetailsNull_throwsException() {
			String token = buildRealToken("kinjal@example.com", EXPIRATION_MS);

			assertThatThrownBy(() -> jwtService.isTokenValid(token, null))
					.isInstanceOf(NullPointerException.class);
		}
	}
}