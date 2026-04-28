package userservice.security;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Service responsible for handling JSON Web Token (JWT) operations such as
 * generation, validation, and claim extraction.
 *
 * <p>
 * This service uses the JJWT library to create and parse JWT tokens signed with
 * an HMAC SHA-256 secret key. It integrates with Spring Security to support
 * stateless authentication.
 *
 * <p>
 * <b>Key Responsibilities:</b>
 * <ul>
 * <li><b>Token Generation:</b> Generates JWT tokens containing:
 * <ul>
 * <li>Subject (user email)</li>
 * <li>Custom claims such as {@code role} and {@code userId}</li>
 * <li>Issued timestamp and expiration time</li>
 * </ul>
 * </li>
 *
 * <li><b>Token Validation:</b> Verifies:
 * <ul>
 * <li>Token belongs to the given user (email match)</li>
 * <li>Token is not expired</li>
 * <li>User account is enabled</li>
 * </ul>
 * </li>
 *
 * <li><b>Claim Extraction:</b> Provides utility methods to extract:
 * <ul>
 * <li>Username (subject)</li>
 * <li>Expiration date</li>
 * <li>Custom claims via functional interface</li>
 * </ul>
 * </li>
 * </ul>
 *
 * <p>
 * <b>Configuration:</b>
 * <ul>
 * <li>{@code jwt.secret} - Base64 encoded secret key used for signing
 * tokens</li>
 * <li>{@code jwt.expiration} - Token validity duration in milliseconds</li>
 * </ul>
 *
 * <p>
 * <b>Security Notes:</b>
 * <ul>
 * <li>Uses {@code HS256} (HMAC SHA-256) for token signing</li>
 * <li>Secret key must be strong and securely stored (e.g., environment
 * variables)</li>
 * <li>Token should not expose sensitive information in claims</li>
 * <li>Always validate token expiration and ownership before granting
 * access</li>
 * </ul>
 *
 * <p>
 * <b>Usage:</b> Typically used in authentication controllers for token
 * generation and in security filters (e.g., JWT filter) for validating incoming
 * requests.
 */
@Service
public class JwtService {

	@Value("${jwt.secret}")
	private String secretKey;

	@Value("${jwt.expiration}")
	private long jwtExpiration;

	/**
	 * Extracts the username (subject) from the provided JWT token.
	 *
	 * <p>
	 * In this application, the username typically represents the authenticated
	 * user's unique email address stored in the token's subject ({@code sub})
	 * claim.
	 *
	 * <p>
	 * This method delegates claim parsing to the generic {@code extractClaim(...)}
	 * utility method and retrieves the subject claim from the token payload.
	 *
	 * <p>
	 * Typical use cases:
	 * <ul>
	 * <li>Identifying the user associated with a token</li>
	 * <li>Authenticating incoming requests</li>
	 * <li>Loading user details from the database</li>
	 * </ul>
	 *
	 * @param token JWT token from which the username is extracted
	 * @return username/email stored in the token subject claim
	 * @throws io.jsonwebtoken.JwtException if token is invalid or malformed
	 * @throws IllegalArgumentException     if token is null or empty
	 */
	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	/**
	 * Extracts a specific claim from the provided JWT token using a resolver
	 * function.
	 *
	 * <p>
	 * This generic utility method parses all claims from the token and applies the
	 * given resolver function to retrieve the required claim value.
	 *
	 * <p>
	 * It enables flexible extraction of different JWT claims such as:
	 * <ul>
	 * <li>Subject (username/email)</li>
	 * <li>Expiration date</li>
	 * <li>Issued timestamp</li>
	 * <li>Custom application claims</li>
	 * </ul>
	 *
	 * <p>
	 * Example usage:
	 * 
	 * <pre>
	 * extractClaim(token, Claims::getSubject);
	 * extractClaim(token, Claims::getExpiration);
	 * </pre>
	 *
	 * @param <T>            type of claim value to be returned
	 * @param token          JWT token containing claims
	 * @param claimsResolver function used to extract a specific claim from parsed
	 *                       {@link Claims}
	 * @return extracted claim value
	 * @throws io.jsonwebtoken.JwtException if token is invalid, expired, tampered,
	 *                                      or malformed
	 * @throws IllegalArgumentException     if token is null or empty
	 */
	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		return claimsResolver.apply(extractAllClaims(token));
	}

	/**
	 * Generates a signed JWT token for the authenticated user.
	 *
	 * <p>
	 * This method creates a token containing standard JWT claims along with custom
	 * application-specific claims required for authorization and user context.
	 *
	 * <p>
	 * Included custom claims:
	 * <ul>
	 * <li><b>role</b> - primary granted authority of the user</li>
	 * <li><b>userId</b> - unique database identifier of the user</li>
	 * </ul>
	 *
	 * <p>
	 * If the provided {@link UserDetails} instance is of type
	 * {@link CustomUserDetails}, additional claims are extracted from the wrapped
	 * domain user object.
	 *
	 * <p>
	 * The generated token is typically used for:
	 * <ul>
	 * <li>User authentication after successful login</li>
	 * <li>Authorization of protected API requests</li>
	 * <li>Stateless session management</li>
	 * </ul>
	 *
	 * @param userDetails authenticated user details used to generate token
	 * @return signed JWT token string
	 * @throws RuntimeException if token signing or claim generation fails
	 */
	public String generateToken(UserDetails userDetails) {

		Map<String, Object> extraClaims = new HashMap<>();

		if (userDetails instanceof CustomUserDetails customUser) {
			extraClaims.put("role", customUser.getAuthorities().iterator().next().getAuthority());
			extraClaims.put("userId", customUser.getUser().getId());
		}
		return buildToken(extraClaims, userDetails);
	}

	/**
	 * Validates whether the provided JWT token is authentic and usable for the
	 * given user.
	 *
	 * <p>
	 * This method performs multiple security checks before accepting the token for
	 * authentication.
	 *
	 * <p>
	 * Validation criteria:
	 * <ul>
	 * <li><b>User match</b> - token subject must match the provided user's
	 * username</li>
	 * <li><b>Not expired</b> - token expiration time must be valid</li>
	 * <li><b>Account enabled</b> - user account must be active/enabled</li>
	 * </ul>
	 *
	 * <p>
	 * If all checks pass, the token is considered valid for the current user.
	 *
	 * <p>
	 * This method is commonly invoked by authentication filters before setting the
	 * security context.
	 *
	 * @param token       JWT token to validate
	 * @param userDetails authenticated user details to compare against token data
	 * @return {@code true} if token is valid and belongs to the user; otherwise
	 *         {@code false}
	 * @throws io.jsonwebtoken.JwtException if token is malformed, tampered, or
	 *                                      unreadable
	 * @throws IllegalArgumentException     if token is null or empty
	 */
	public boolean isTokenValid(String token, UserDetails userDetails) {
		final String email = extractUsername(token);
		boolean isSameUser = email.equals(userDetails.getUsername());
		boolean notExpired = !isTokenExpired(token);
		boolean enabled = userDetails.isEnabled();

		return isSameUser && notExpired && enabled;
	}

	/**
	 * Builds and signs a JWT token using the provided claims and user details.
	 *
	 * <p>
	 * This internal helper method creates the final token payload by adding:
	 * <ul>
	 * <li><b>Custom claims</b> supplied by the caller</li>
	 * <li><b>Subject</b> representing the authenticated username/email</li>
	 * <li><b>Issued time</b> indicating token creation timestamp</li>
	 * <li><b>Expiration time</b> based on configured token validity period</li>
	 * </ul>
	 *
	 * <p>
	 * The token is cryptographically signed using the configured secret key and
	 * HMAC SHA-256 ({@code HS256}) algorithm to ensure integrity and authenticity.
	 *
	 * <p>
	 * This method is typically used internally by token generation methods after
	 * preparing standard and custom claims.
	 *
	 * @param extraClaims additional claims to include in the token payload
	 * @param userDetails authenticated user details whose username is used as the
	 *                    JWT subject
	 * @return compact serialized JWT token string
	 * @throws RuntimeException if signing key is invalid or token creation fails
	 */
	private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails) {
		return Jwts.builder().setClaims(extraClaims).setSubject(userDetails.getUsername())
				.setIssuedAt(new Date(System.currentTimeMillis()))
				.setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
				.signWith(getSigningKey(), SignatureAlgorithm.HS256).compact();
	}

	/**
	 * Determines whether the provided JWT token has expired.
	 *
	 * <p>
	 * This method compares the token's expiration claim with the current system
	 * time. If the expiration timestamp is earlier than the current date/time, the
	 * token is considered expired.
	 *
	 * <p>
	 * Expired tokens should no longer be accepted for authentication or
	 * authorization purposes.
	 *
	 * @param token JWT token to evaluate
	 * @return {@code true} if the token has expired; {@code false} if it is still
	 *         valid by time
	 * @throws io.jsonwebtoken.JwtException if token is malformed or cannot be
	 *                                      parsed
	 * @throws IllegalArgumentException     if token is null or empty
	 */
	private boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date());
	}

	/**
	 * Extracts the expiration timestamp from the provided JWT token.
	 *
	 * <p>
	 * This method reads the standard JWT {@code exp} claim, which defines the exact
	 * date and time after which the token becomes invalid.
	 *
	 * <p>
	 * Typically used for:
	 * <ul>
	 * <li>Token validity checks</li>
	 * <li>Session timeout handling</li>
	 * <li>Authentication filter validation</li>
	 * </ul>
	 *
	 * @param token JWT token containing expiration metadata
	 * @return token expiration date and time
	 * @throws io.jsonwebtoken.JwtException if token is invalid, tampered, or
	 *                                      unreadable
	 * @throws IllegalArgumentException     if token is null or empty
	 */
	private Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}

	/**
	 * Parses the provided JWT token and extracts all claims from its payload.
	 *
	 * <p>
	 * This method validates the token signature using the configured signing key
	 * before returning the claims body. If the token has been tampered with,
	 * expired, malformed, or signed with an invalid key, parsing will fail with an
	 * exception.
	 *
	 * <p>
	 * Returned claims may include:
	 * <ul>
	 * <li>Subject (username/email)</li>
	 * <li>Issued timestamp</li>
	 * <li>Expiration timestamp</li>
	 * <li>Custom claims such as role or userId</li>
	 * </ul>
	 *
	 * @param token JWT token to parse
	 * @return all validated claims contained in the token
	 * @throws io.jsonwebtoken.JwtException if token is expired, malformed,
	 *                                      unsupported, or signature validation
	 *                                      fails
	 * @throws IllegalArgumentException     if token is null or empty
	 */
	private Claims extractAllClaims(String token) {
		return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
	}

	/**
	 * Generates the cryptographic signing key used for JWT signing and signature
	 * verification.
	 *
	 * <p>
	 * The key is derived from the configured Base64-encoded secret stored in
	 * application properties or environment variables.
	 *
	 * <p>
	 * This key is used with the HMAC SHA family of algorithms (such as HS256) for
	 * secure token generation and validation.
	 *
	 * <p>
	 * Security recommendation:
	 * <ul>
	 * <li>Use a strong randomly generated secret</li>
	 * <li>Store secrets in environment variables or secret managers</li>
	 * <li>Rotate keys periodically in production</li>
	 * </ul>
	 *
	 * @return signing key for JWT operations
	 * @throws IllegalArgumentException if secret key is invalid or empty
	 */
	private Key getSigningKey() {
		return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
	}
}