package userservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuration class for Spring Security setup.
 *
 * <p>This class defines the security configuration for the application,
 * including authentication, authorization, password encoding, and JWT-based
 * stateless session management.
 *
 * <p><b>Key Responsibilities:</b>
 * <ul>
 *   <li><b>Password Encoding:</b>
 *       Configures {@link BCryptPasswordEncoder} to securely hash user passwords.</li>
 *
 *   <li><b>Authentication Provider:</b>
 *       Uses {@link DaoAuthenticationProvider} with {@link CustomUserDetailsService}
 *       to authenticate users against the database.</li>
 *
 *   <li><b>Authentication Manager:</b>
 *       Exposes {@link AuthenticationManager} bean required for handling authentication logic
 *       (e.g., login API).</li>
 *
 *   <li><b>Security Filter Chain:</b>
 *       Configures HTTP security:
 *       <ul>
 *         <li>Disables CSRF (suitable for stateless APIs)</li>
 *         <li>Disables HTTP Basic authentication</li>
 *         <li>Enforces stateless session management using JWT</li>
 *         <li>Defines public and secured endpoints</li>
 *         <li>Registers custom {@link JwtAuthenticationFilter} before
 *             {@link UsernamePasswordAuthenticationFilter}</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <p><b>Authorization Rules:</b>
 * <ul>
 *   <li>Public (no authentication required):
 *       <ul>
 *         <li>{@code /api/v1/users/login}</li>
 *         <li>{@code /api/v1/users/register}</li>
 *         <li>Swagger/OpenAPI endpoints</li>
 *       </ul>
 *   </li>
 *   <li>All other endpoints require authentication</li>
 * </ul>
 *
 * <p><b>JWT Integration:</b>
 * <ul>
 *   <li>Uses {@link JwtAuthenticationFilter} to intercept requests and validate JWT tokens</li>
 *   <li>Ensures stateless authentication (no HTTP session is maintained)</li>
 * </ul>
 *
 * <p><b>Security Notes:</b>
 * <ul>
 *   <li>CSRF is disabled because JWT is used instead of cookies</li>
 *   <li>Password must always be stored in encoded (hashed) form</li>
 *   <li>Ensure secure storage of JWT secret key and proper token expiration</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * Automatically picked up by Spring Boot as a {@code @Configuration} class
 * to initialize application-wide security settings.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final CustomUserDetailsService customUserDetailsService;
	private final JwtAuthenticationFilter jwtAuthenticationFilter; // Inject filter

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(customUserDetailsService);
		provider.setPasswordEncoder(passwordEncoder());
		return provider;
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http
				.csrf(csrf -> csrf.disable())
				.httpBasic(basic -> basic.disable())
				.authenticationProvider(authenticationProvider())
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				)
				.authorizeHttpRequests(auth -> auth
						// Public endpoints
						.requestMatchers(
								"/api/v1/users/login",
								"/api/v1/users/register",
								"/v3/api-docs/**",
								"/swagger-ui/**",
								"/swagger-ui.html"
						).permitAll()

						//  Everything else secured
						.anyRequest().authenticated()
				)
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}