package Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos;

import java.time.LocalDateTime;

import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponseDTO {
	private Long id;
	private String email;
	private Role role;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private SimpleUserDTO createdBy;
	private SimpleUserDTO updatedBy;

}
