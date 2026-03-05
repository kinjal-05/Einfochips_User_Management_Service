package Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.enums.Role;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.models.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public class UserSpecification {

	public static Specification<User> filterUsers(String email, Role role, String createdByEmail, String updatedByEmail,
			LocalDateTime fromDate, LocalDateTime toDate) {

		return (root, query, cb) -> {

			List<Predicate> predicates = new ArrayList<>();
			if (email != null && !email.isEmpty()) {
				predicates.add(cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
			}

			if (role != null) {
				predicates.add(cb.equal(root.get("role"), role));
			}
			if (createdByEmail != null && !createdByEmail.isEmpty()) {
				Join<User, User> createdByJoin = root.join("createdBy", JoinType.LEFT);
				predicates.add(cb.like(cb.lower(createdByJoin.get("email")), "%" + createdByEmail.toLowerCase() + "%"));
			}
			if (updatedByEmail != null && !updatedByEmail.isEmpty()) {
				Join<User, User> updatedByJoin = root.join("updatedBy", JoinType.LEFT);
				predicates.add(cb.like(cb.lower(updatedByJoin.get("email")), "%" + updatedByEmail.toLowerCase() + "%"));
			}

			if (fromDate != null && toDate != null) {
				predicates.add(cb.between(root.get("createdAt"), fromDate, toDate));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}