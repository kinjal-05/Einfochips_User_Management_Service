package Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos;

import java.time.LocalDateTime;

import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.enums.Role;
import lombok.Builder;
import lombok.Data;

public record DeleteResponseDTO(
		long id,
		String email,
		Role role,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		long createdById,
		long updatedById,
		boolean isDeleted,
		String message
) {}
