package Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.controllers;

import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
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

import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.enums.Role;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final PasswordEncoder passwordEncoder;

	@PostMapping("/registerUser")
	public ResponseEntity<UserResponseDTO> registerUser(@RequestBody @Valid UserRequestDTO request) {
			UserResponseDTO response = userService.registerUser(request);
			return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponseDTO> login(@Validated @RequestBody LoginRequestDTO request) {
		return ResponseEntity.ok(userService.login(request));
	}

	@PostMapping("/search")
	public Page<UserResponseDTO> searchUsers(
			@RequestBody UserSearchRequestDTO request,
			Pageable pageable) {

		return userService.searchUsers(request, pageable);
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

	@PatchMapping("/changePassword")
	public ResponseEntity<ChangePasswordResponseDTO> changePassword(
			@RequestBody @Valid ChangePasswordRequestDTO request) {
		return ResponseEntity.ok(userService.changePassword(request));
	}

}
