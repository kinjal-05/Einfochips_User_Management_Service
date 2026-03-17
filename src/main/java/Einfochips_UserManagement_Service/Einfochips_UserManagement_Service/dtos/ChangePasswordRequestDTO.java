package Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos;

public record ChangePasswordRequestDTO(
		String oldPassword,
		String newPassword
) {}