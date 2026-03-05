package Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.services;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos.DeleteResponseDTO;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos.LoginRequestDTO;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos.LoginResponseDTO;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos.UserRequestDTO;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos.UserResponseDTO;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos.UserUpdateRequestDTO;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.enums.Role;

public interface UserService {
	UserResponseDTO registerUser(UserRequestDTO request);

	LoginResponseDTO login(LoginRequestDTO request);

	Page<UserResponseDTO> searchUsers(String email, Role role, String createdBy, String updatedBy,
			LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable);

	UserResponseDTO updateUser(Long id, UserUpdateRequestDTO request);

	UserResponseDTO getUserById(Long id);

	DeleteResponseDTO softDeleteUser(Long id);
}
