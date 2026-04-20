package userservice.repositories;

import java.util.List;
import java.util.Optional;

import userservice.dtos.UserEmailRoleProjection;
import userservice.models.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long>,
		JpaSpecificationExecutor<User> {

	/*
	 * ROOT CAUSE OF SILENT EMPTY RESULT:
	 *
	 * Native query:
	 *   SELECT * FROM users WHERE um_email = ?
	 *
	 * Returns raw DB columns (um_id, um_email, um_is_deleted, etc.)
	 * Hibernate tries to map um_is_deleted → isDeleted field.
	 * When mapping fails silently, Hibernate returns Optional.empty()
	 * instead of throwing an error — so no exception, just empty result.
	 *
	 * FIX: Use JPQL instead of native SQL.
	 * JPQL works with entity field names (isDeleted) not column names (um_is_deleted).
	 * Hibernate handles the column mapping automatically and correctly.
	 *
	 * IMPORTANT: @Where on entity adds filter to JPQL queries too.
	 * So we must also remove @Where from User entity (already done).
	 * Without @Where, this JPQL finds ALL users including deleted ones.
	 */

	// For authentication — finds user by email WITHOUT isDeleted filter
	// Used ONLY by CustomUserDetailsService
	// Deleted users found here are rejected by CustomUserDetails.isEnabled() = false
	@Query("SELECT u FROM User u WHERE u.email = :email")
	Optional<User> findByEmail(@Param("email") String email);

	// For business logic — finds ONLY active (non-deleted) users
	// Use this in UserServiceImpl for all non-auth operations
	@Query("SELECT u FROM User u WHERE u.email = :email AND u.isDeleted = false")
	Optional<User> findActiveByEmail(@Param("email") String email);

	// Find active user by ID
	@Query("SELECT u FROM User u WHERE u.id = :id AND u.isDeleted = false")
	Optional<User> findActiveById(@Param("id") long id);

	//  Projection query
	@Query("SELECT u.email AS email, u.role AS role FROM User u WHERE u.email = :email AND u.isDeleted = false")
	Optional<UserEmailRoleProjection> findEmailAndRoleByEmail(@Param("email") String email);

	// All active users
	List<User> findByIsDeletedFalse();

	// Email exists check — active users only
	@Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.isDeleted = false")
	boolean existsByEmail(@Param("email") String email);
}