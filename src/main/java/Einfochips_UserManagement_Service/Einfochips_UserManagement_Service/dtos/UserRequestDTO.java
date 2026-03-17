package Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos;

import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

public record UserRequestDTO(

		@Email(message = "Invalid Email Format")
		@NotBlank(message = "Email is Required")
		String email,

		@NotNull(message = "Role is required")
		Role role

) {}