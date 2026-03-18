package userservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final CustomUserDetailsService customUserDetailsService;

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain
	) throws ServletException, IOException {

		final String requestPath = request.getServletPath();
		log.info("=== JwtAuthenticationFilter: path={}", requestPath);

		/*
		 * FIX: Skip JWT validation for public endpoints.
		 *
		 * ROOT CAUSE OF DOUBLE QUERY:
		 * JwtAuthenticationFilter was processing ALL requests including /login.
		 * For /login request:
		 *   - Filter runs → no JWT token → tries to load user anyway
		 *   - First Hibernate query fires (from filter)
		 *   - Then authenticationManager.authenticate() fires
		 *   - Second Hibernate query fires (from DaoAuthenticationProvider)
		 *
		 * Both queries return empty because the filter was corrupting
		 * the SecurityContext before authentication could complete.
		 *
		 * FIX: Return early for public endpoints — let them pass through
		 * without any JWT processing.
		 */
		if (isPublicEndpoint(requestPath)) {
			log.info("=== Skipping JWT filter for public endpoint: {}", requestPath);
			filterChain.doFilter(request, response);
			return;
		}

		// Extract Authorization header
		final String authHeader = request.getHeader("Authorization");

		// No token present — pass to next filter
		// Spring Security will reject unauthenticated access to secured endpoints
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			log.info("=== No JWT token found for path: {}", requestPath);
			filterChain.doFilter(request, response);
			return;
		}

		// Extract token from header
		final String jwt = authHeader.substring(7);
		final String userEmail;

		try {
			userEmail = jwtService.extractUsername(jwt);
		} catch (Exception e) {
			log.error("=== Failed to extract username from JWT: {}", e.getMessage());
			filterChain.doFilter(request, response);
			return;
		}

		// Validate token and set authentication in SecurityContext
		if (userEmail != null
				&& SecurityContextHolder.getContext().getAuthentication() == null) {

			UserDetails userDetails =
					customUserDetailsService.loadUserByUsername(userEmail);

			if (jwtService.isTokenValid(jwt, userDetails)) {
				UsernamePasswordAuthenticationToken authToken =
						new UsernamePasswordAuthenticationToken(
								userDetails,
								null,
								userDetails.getAuthorities()
						);
				authToken.setDetails(
						new WebAuthenticationDetailsSource().buildDetails(request)
				);
				SecurityContextHolder.getContext().setAuthentication(authToken);
				log.info("=== JWT valid — authenticated user: {}", userEmail);
			} else {
				log.warn("=== JWT invalid for user: {}", userEmail);
			}
		}

		filterChain.doFilter(request, response);
	}

	/**
	 * Returns true for endpoints that do not require JWT authentication.
	 * These endpoints are skipped by the JWT filter entirely.
	 */
	private boolean isPublicEndpoint(String path) {
		return path.startsWith("/api/v1/users/login") ||
				path.startsWith("/api/v1/users/encode") ||
				path.startsWith("/swagger-ui") ||
				path.startsWith("/v3/api-docs");
	}
}