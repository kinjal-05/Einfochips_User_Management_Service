package Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos;

import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.enums.Role;
import lombok.Data;

@Data
public class UserUpdateRequestDTO {
	private String email;
	private Role role;
	private Boolean isDeleted;
}
