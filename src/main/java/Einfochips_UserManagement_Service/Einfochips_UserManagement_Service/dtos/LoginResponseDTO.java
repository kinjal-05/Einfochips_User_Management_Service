package Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos;

import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public record LoginResponseDTO(
		long id,
		String email,
		Role role,
		String token,
		String message
) {}