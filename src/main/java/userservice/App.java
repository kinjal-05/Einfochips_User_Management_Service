package userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the User Service application.
 *
 * <p>
 * This class bootstraps the Spring Boot application and initializes:
 * <ul>
 * <li>Spring Application Context</li>
 * <li>Auto-configuration based on project dependencies</li>
 * <li>Embedded web server (Tomcat/Jetty/Undertow)</li>
 * <li>Component scanning for beans, services, repositories, and
 * controllers</li>
 * </ul>
 *
 * <p>
 * The {@code @SpringBootApplication} annotation is a convenience annotation
 * that combines:
 * <ul>
 * <li>{@code @Configuration}</li>
 * <li>{@code @EnableAutoConfiguration}</li>
 * <li>{@code @ComponentScan}</li>
 * </ul>
 *
 * <p>
 * This is the starting point of the microservice and should remain lightweight,
 * containing only application startup logic.
 *
 * @author Kinjal Mistry
 * @version 1.0
 * @since 1.0
 */
@SpringBootApplication
public class App {

	/**
	 * Starts the User Service application.
	 *
	 * @param args command-line arguments passed during startup
	 */
	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}
}
