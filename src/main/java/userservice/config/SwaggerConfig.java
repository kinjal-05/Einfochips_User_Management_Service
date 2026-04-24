package userservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for setting up OpenAPI (Swagger) documentation.
 *
 * <p>
 * This class defines the OpenAPI specification for the application using
 * Springdoc. It provides metadata about the API and configures security
 * using JWT-based authentication.
 * </p>
 *
 * <p>
 * <b>Key Features:</b>
 * <ul>
 *   <li>Defines API metadata such as title, description, and version.</li>
 *   <li>Configures JWT-based authentication using Bearer token scheme.</li>
 *   <li>Adds a global security requirement so that all endpoints
 *       can be secured using the Authorization header.</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>Security Configuration:</b>
 * <ul>
 *   <li>Uses HTTP Bearer authentication scheme.</li>
 *   <li>Expects JWT tokens in the {@code Authorization} header.</li>
 *   <li>Format: {@code Authorization: Bearer <token>}</li>
 * </ul>
 * </p>
 *
 * <p>
 * This configuration enables interactive API documentation via Swagger UI,
 * allowing developers to test secured endpoints by providing a JWT token.
 * </p>
 */
@Configuration
public class SwaggerConfig {


	@Bean
	public OpenAPI openAPI() {


		return new OpenAPI()

				.info(new Info()
						.title("User Service API")
						.description("API documentation with JWT authentication")
						.version("1.0"))

				.addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
				.components(new Components()
						.addSecuritySchemes("BearerAuth",
								new SecurityScheme()

										.name("Authorization")

										.type(SecurityScheme.Type.HTTP)

										.scheme("bearer")

										.bearerFormat("JWT")
						)
				);
	}
}
