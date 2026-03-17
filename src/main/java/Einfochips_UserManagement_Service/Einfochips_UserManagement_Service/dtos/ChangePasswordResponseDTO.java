package Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos;

public record ChangePasswordResponseDTO(
		Long userId,
		String email,
		String message
) {}