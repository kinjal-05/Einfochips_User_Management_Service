package userservice.models;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import userservice.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql="UPDATE users SET um_is_deleted = true, um_deleted_timestamp = CURRENT_TIMESTAMP WHERE um_id = ?")
@Builder
@DynamicUpdate
@DynamicInsert
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

	@CreatedDate
	@Column(name = "um_created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "um_updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Column(name = "um_created_by")
	@CreatedBy
	private long createdById;

	@Column(name = "um_updated_by")
	@LastModifiedBy
	private long updatedById;

	@Column(name = "um_is_deleted", nullable = false)
	private boolean isDeleted = false;

	@Column(name = "um_deleted_timestamp")
	private LocalDateTime deletedTimestamp;

}