package userservice.security;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * JWT authentication filter responsible for validating incoming JSON Web Tokens
 * and establishing authenticated user sessions in the Spring Security context.
 *
 * <p>
 * This filter executes once per HTTP request and performs the following:
 * <ul>
 * <li>Skips authentication for publicly accessible endpoints</li>
 * <li>Extracts JWT token from the Authorization header</li>
 * <li>Validates token integrity and expiration</li>
 * <li>Loads user details associated with the token</li>
 * <li>Creates and stores authentication object in SecurityContext</li>
 * </ul>
 *
 * <p>
 * Expected Authorization header format:
 * 
 * <pre>
 * Authorization: Bearer &lt;jwt-token&gt;
 * </pre>
 *
 * <p>
 * If the token is missing or invalid, the request continues through the filter
 * chain without authentication. Access decisions are then handled by Spring
 * Security configuration.
 *
 * <p>
 * This class extends {@link OncePerRequestFilter} to ensure execution only once
 * per request lifecycle.
 *
 * @author Kinjal Mistry
 * @version 1.0
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final CustomUserDetailsService customUserDetailsService;

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {

		final String requestPath = request.getServletPath();

		/*
		 * FIX: Skip JWT validation for public endpoints.
		 *
		 * ROOT CAUSE OF DOUBLE QUERY: JwtAuthenticationFilter was processing ALL
		 * requests including /login. For /login request: - Filter runs → no JWT token →
		 * tries to load user anyway - First Hibernate query fires (from filter) - Then
		 * authenticationManager.authenticate() fires - Second Hibernate query fires
		 * (from DaoAuthenticationProvider)
		 *
		 * Both queries return empty because the filter was corrupting the
		 * SecurityContext before authentication could complete.
		 *
		 * FIX: Return early for public endpoints — let them pass through without any
		 * JWT processing.
		 */
		if (isPublicEndpoint(requestPath)) {
			filterChain.doFilter(request, response);
			return;
		}

		// Extract Authorization header
		final String authHeader = request.getHeader("Authorization");

		// No token present — pass to next filter
		// Spring Security will reject unauthenticated access to secured endpoints
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		// Extract token from header
		final String jwt = authHeader.substring(7);
		final String userEmail;

		try {
			userEmail = jwtService.extractUsername(jwt);
		} catch (Exception e) {
			filterChain.doFilter(request, response);
			return;
		}

		// Validate token and set authentication in SecurityContext
		if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

			UserDetails userDetails = customUserDetailsService.loadUserByUsername(userEmail);

			if (jwtService.isTokenValid(jwt, userDetails)) {
				UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
						null, userDetails.getAuthorities());
				authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authToken);
			} else {

			}
		}

		filterChain.doFilter(request, response);
	}

	/**
	 * Returns true for endpoints that do not require JWT authentication. These
	 * endpoints are skipped by the JWT filter entirely.
	 */
	private boolean isPublicEndpoint(String path) {
		return path.startsWith("/api/v1/users/login") || path.startsWith("/api/v1/users/encode")
				|| path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs");
	}
}