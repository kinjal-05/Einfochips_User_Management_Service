package Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos;

import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.enums.Role;
import lombok.Data;

import jakarta.validation.constraints.Email;

public record UserUpdateRequestDTO(
		@Email(message = "Invalid Email Format")
		String email,
		Role role
) {}
