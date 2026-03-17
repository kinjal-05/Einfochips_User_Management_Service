package Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.repositories;

import java.util.List;
import java.util.Optional;

import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos.UserEmailRoleProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.models.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

	Optional<User> findByEmail(String email);

	@Query("SELECT u.email AS email, u.role AS role FROM User u WHERE u.email = :email")
	Optional<UserEmailRoleProjection> findEmailAndRoleByEmail(@Param("email") String email);

	List<User> findByIsDeletedFalse();
	boolean existsByEmail(String email);
}
