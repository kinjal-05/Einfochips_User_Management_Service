package userservice.models;

import java.time.LocalDateTime;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import userservice.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Where;

@Entity
@Table(
		name = "users",
		uniqueConstraints = {
				@UniqueConstraint(columnNames = {"um_email", "um_deleted_timestamp"})
		}
)
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "um_id", updatable = false, nullable = false)
	@EqualsAndHashCode.Include
	private long id;

	@Email(message = "Invalid Email Format")
	@NotBlank(message = "Email is Required")
	@Column(name = "um_email", nullable = false, length = 150)
	private String email;

	@NotBlank(message = "Password is Required")
	@Size(min = 8, message = "Password must be 8 characters")
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

	@Column(name = "um_created_by")
	private long createdById;

	@Column(name = "um_updated_by")
	private long updatedById;

	@Column(name = "um_is_deleted", nullable = false)
	private boolean isDeleted = false;

	@Column(name = "um_deleted_timestamp")
	private LocalDateTime deletedTimestamp;

}