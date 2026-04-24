package userservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service responsible for handling JSON Web Token (JWT) operations
 * such as generation, validation, and claim extraction.
 *
 * <p>This service uses the JJWT library to create and parse JWT tokens
 * signed with an HMAC SHA-256 secret key. It integrates with Spring Security
 * to support stateless authentication.
 *
 * <p><b>Key Responsibilities:</b>
 * <ul>
 *   <li><b>Token Generation:</b>
 *       Generates JWT tokens containing:
 *       <ul>
 *         <li>Subject (user email)</li>
 *         <li>Custom claims such as {@code role} and {@code userId}</li>
 *         <li>Issued timestamp and expiration time</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>Token Validation:</b>
 *       Verifies:
 *       <ul>
 *         <li>Token belongs to the given user (email match)</li>
 *         <li>Token is not expired</li>
 *         <li>User account is enabled</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>Claim Extraction:</b>
 *       Provides utility methods to extract:
 *       <ul>
 *         <li>Username (subject)</li>
 *         <li>Expiration date</li>
 *         <li>Custom claims via functional interface</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <p><b>Configuration:</b>
 * <ul>
 *   <li>{@code jwt.secret} - Base64 encoded secret key used for signing tokens</li>
 *   <li>{@code jwt.expiration} - Token validity duration in milliseconds</li>
 * </ul>
 *
 * <p><b>Security Notes:</b>
 * <ul>
 *   <li>Uses {@code HS256} (HMAC SHA-256) for token signing</li>
 *   <li>Secret key must be strong and securely stored (e.g., environment variables)</li>
 *   <li>Token should not expose sensitive information in claims</li>
 *   <li>Always validate token expiration and ownership before granting access</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * Typically used in authentication controllers for token generation
 * and in security filters (e.g., JWT filter) for validating incoming requests.
 */
@Service
public class JwtService {

	@Value("${jwt.secret}")
	private String secretKey;

	@Value("${jwt.expiration}")
	private long jwtExpiration;

	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		return claimsResolver.apply(extractAllClaims(token));
	}

	public String generateToken(UserDetails userDetails) {

		Map<String, Object> extraClaims = new HashMap<>();

		if (userDetails instanceof CustomUserDetails customUser) {
			extraClaims.put("role", customUser.getAuthorities()
					.iterator().next().getAuthority());
			extraClaims.put("userId", customUser.getUser().getId());
		}
		return buildToken(extraClaims, userDetails);
	}

	public boolean isTokenValid(String token, UserDetails userDetails) {
		final String email = extractUsername(token);
		boolean isSameUser = email.equals(userDetails.getUsername());
		boolean notExpired = !isTokenExpired(token);
		boolean enabled = userDetails.isEnabled();

		return isSameUser && notExpired && enabled;
	}

	private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails) {
		return Jwts.builder()
				.setClaims(extraClaims)
				.setSubject(userDetails.getUsername())
				.setIssuedAt(new Date(System.currentTimeMillis()))
				.setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
				.signWith(getSigningKey(), SignatureAlgorithm.HS256)
				.compact();
	}

	private boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date());
	}

	private Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}

	private Claims extractAllClaims(String token) {
		return Jwts.parserBuilder()
				.setSigningKey(getSigningKey())
				.build()
				.parseClaimsJws(token)
				.getBody();
	}

	private Key getSigningKey() {
		return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
	}
}