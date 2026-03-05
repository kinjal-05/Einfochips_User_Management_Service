package Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos;

import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.enums.Role;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.models.User;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SimpleUserDTO {
	private Long id;
	private String email;
	private Role role;

	public static SimpleUserDTO fromEntity(User user) {
		return SimpleUserDTO.builder().id(user.getId()).email(user.getEmail()).role(user.getRole()).build();
	}
}