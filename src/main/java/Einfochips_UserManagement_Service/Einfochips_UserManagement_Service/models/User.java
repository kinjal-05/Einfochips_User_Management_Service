package Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.models;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users", uniqueConstraints = { @UniqueConstraint(columnNames = "email") })
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "um_id", updatable = false, nullable = false)
	private Long id;

	@Email(message = "Invalid Email Format")
	@NotBlank(message = "Email is Required")
	@Column(name = "um_email", nullable = false, unique = true, length = 150)
	private String email;

	@NotBlank(message = "Password is Required")
	@Size(min = 8, message = "Passowrd must be 8 characters")
	@Column(name = "um_password", nullable = false)
	@JsonIgnore
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(name = "um_role", nullable = false)
	private Role role;

	@CreationTimestamp
	@Column(name = "um_created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "um_updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "um_created_by")
	@JsonIgnore
	private User createdBy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "um_updated_by")
	@JsonIgnore
	private User updatedBy;

	@Column(name = "um_is_deleted", nullable = false)
	private boolean isDeleted = false;

}