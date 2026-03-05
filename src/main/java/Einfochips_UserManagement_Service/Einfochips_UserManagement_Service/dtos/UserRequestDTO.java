package Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRequestDTO {

	@Email(message = "Invalid Email Format")
	@NotBlank(message = "Email is Required")
	private String email;

	@NotBlank(message = "Password is Required")
	@Size(min = 8, message = "Password must be at least 8 characters")
	private String password;
}
