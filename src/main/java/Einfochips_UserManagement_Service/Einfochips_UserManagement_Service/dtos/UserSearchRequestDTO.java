package Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos;

import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.enums.Role;

import java.time.LocalDateTime;

public record UserSearchRequestDTO(
		String email,
		Role role,
		long createdById,
		long updatedById,
		LocalDateTime fromDate,
		LocalDateTime toDate
) {}