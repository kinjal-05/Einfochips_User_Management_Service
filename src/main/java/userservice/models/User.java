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


/**
 * Entity representing a User in the system.
 *
 * <p>This class maps to the {@code users} table and stores authentication,
 * authorization, and auditing information for each user.
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Unique Constraint:</b> Ensures that email is unique among active users
 *       by combining {@code um_email} and {@code um_deleted_timestamp}. This allows
 *       reuse of the same email after soft deletion.</li>
 *
 *   <li><b>Validation:</b> Uses Bean Validation annotations such as {@code @Email},
 *       {@code @NotBlank}, and {@code @Size} to enforce data integrity at the API level.</li>
 *
 *   <li><b>Security:</b> The password field is annotated with {@code @JsonIgnore}
 *       to prevent exposure in API responses.</li>
 *
 *   <li><b>Role Management:</b> Stores user roles using {@code EnumType.STRING}
 *       for better readability and maintainability.</li>
 *
 *   <li><b>Auditing:</b> Integrated with {@link AuditingEntityListener} to automatically
 *       populate:
 *       <ul>
 *         <li>{@code createdAt}, {@code updatedAt}</li>
 *         <li>{@code createdById}, {@code updatedById}</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>Soft Delete:</b>
 *       Instead of physically deleting records, users are marked as deleted using:
 *       <ul>
 *         <li>{@code isDeleted} flag</li>
 *         <li>{@code deletedTimestamp}</li>
 *       </ul>
 *       This ensures data retention and auditability.</li>
 *
 *   <li><b>Performance Optimization:</b>
 *       <ul>
 *         <li>{@code @DynamicInsert}: Includes only non-null fields in INSERT queries</li>
 *         <li>{@code @DynamicUpdate}: Updates only modified fields in UPDATE queries</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <p><b>Database Columns Mapping:</b>
 * <ul>
 *   <li>{@code um_id} - Primary key</li>
 *   <li>{@code um_email} - User email (unique with soft delete support)</li>
 *   <li>{@code um_password} - Encrypted password</li>
 *   <li>{@code um_role} - User role</li>
 *   <li>{@code um_created_at}, {@code um_updated_at} - Audit timestamps</li>
 *   <li>{@code um_created_by}, {@code um_updated_by} - Audit user references</li>
 *   <li>{@code um_is_deleted} - Soft delete flag</li>
 *   <li>{@code um_deleted_timestamp} - Soft delete timestamp</li>
 * </ul>
 *
 * <p><b>Note:</b> This entity relies on Spring Data JPA auditing configuration
 * (e.g., {@code @EnableJpaAuditing}) and a proper implementation of
 * {@code AuditorAware} to populate audit fields.
 */
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