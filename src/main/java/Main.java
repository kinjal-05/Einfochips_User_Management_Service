import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class Main {
	public static void main(String[] args) {
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

		String raw = "Kinu@1234";
		String encodedFromDB = "$2a$10$7QJ9mJ5QJrYFZc8ZC2Pj3eLrL5WJ5zKQ4JH7vR2W9YzJkYpV1X8yK";

		System.out.println(encoder.encode("Admin@123"));
	}
}
