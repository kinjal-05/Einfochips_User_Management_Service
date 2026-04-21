package userservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("App (Spring Boot Main Class) Tests")
class AppTest {

	// ✅ 1. CONTEXT LOAD TEST
	@Test
	@DisplayName("Application context should load successfully")
	void contextLoads() {
		// Passes if Spring Boot starts without errors
	}



	// ✅ 3. MAIN METHOD WITH ARGUMENTS
	@Test
	@DisplayName("Main method should run with arguments")
	void main_WithArguments_ShouldRun() {
		assertDoesNotThrow(() ->
				App.main(new String[]{"--spring.profiles.active=test"})
		);
	}


		@Test
		@DisplayName("Main should handle null args")
		void main_WithNullArgs_ShouldRun() {
			assertThrows(IllegalArgumentException.class,() -> App.main(null));
		}

}