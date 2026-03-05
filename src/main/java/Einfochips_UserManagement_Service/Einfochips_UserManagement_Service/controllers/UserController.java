package Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos.DeleteResponseDTO;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos.LoginRequestDTO;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos.LoginResponseDTO;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos.UserRequestDTO;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos.UserResponseDTO;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos.UserUpdateRequestDTO;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.enums.Role;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@PostMapping("/registerUser")
	@ResponseStatus(HttpStatus.CREATED)
	public UserResponseDTO registerUser(@Valid @RequestBody UserRequestDTO request) {
		return userService.registerUser(request);
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponseDTO> login(@Validated @RequestBody LoginRequestDTO request) {
		return ResponseEntity.ok(userService.login(request));
	}

	@GetMapping("/search")
	public Page<UserResponseDTO> searchUsers(@RequestParam(required = false) String email,
			@RequestParam(required = false) String role, @RequestParam(required = false) Long createdBy,
			@RequestParam(required = false) Long updatedBy, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		Pageable pageable = PageRequest.of(page, size);

		Role roleEnum = role != null ? Role.valueOf(role) : null;

		return userService.searchUsers(email, roleEnum, createdBy != null ? String.valueOf(createdBy) : null,
				updatedBy != null ? String.valueOf(updatedBy) : null, null, null,

				pageable);
	}

	@PatchMapping("/updateUser/{id}")
	public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long id,
			@RequestBody UserUpdateRequestDTO request) {
		UserResponseDTO updatedUser = userService.updateUser(id, request);
		return ResponseEntity.ok(updatedUser);
	}

	@GetMapping("/getById/{id}")
	public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
		UserResponseDTO user = userService.getUserById(id);
		return ResponseEntity.ok(user);
	}

	@DeleteMapping("/deleteUser/{id}")
	public ResponseEntity<DeleteResponseDTO> softDeleteUser(@PathVariable Long id) {
		DeleteResponseDTO response = userService.softDeleteUser(id);
		return ResponseEntity.ok(response);
	}
}
