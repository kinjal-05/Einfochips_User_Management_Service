package Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.services;

import java.time.LocalDateTime;

import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.enums.Role;

public interface UserService {
	UserResponseDTO registerUser(UserRequestDTO request);

	LoginResponseDTO login(LoginRequestDTO request);

	Page<UserResponseDTO> searchUsers(UserSearchRequestDTO request, Pageable pageable);

	UserResponseDTO updateUser(long id, UserUpdateRequestDTO request);

	UserResponseDTO getUserById(long id);

	DeleteResponseDTO softDeleteUser(long id);

	ChangePasswordResponseDTO changePassword( ChangePasswordRequestDTO request);
}
