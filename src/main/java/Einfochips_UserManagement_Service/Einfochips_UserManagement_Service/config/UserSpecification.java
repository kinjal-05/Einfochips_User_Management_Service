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

	public static Specification<User> filterUsers(
			String email,
			Role role,
			Long createdById,
			Long updatedById,
			LocalDateTime fromDate,
			LocalDateTime toDate) {

		return (root, query, cb) -> {

			List<Predicate> predicates = new ArrayList<>();

			if (email != null) {
				predicates.add(cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
			}

			if (role != null) {
				predicates.add(cb.equal(root.get("role"), role));
			}

			if (createdById != null) {
				predicates.add(cb.equal(root.get("createdById"), createdById));
			}

			if (updatedById != null) {
				predicates.add(cb.equal(root.get("updatedById"), updatedById));
			}

			if (fromDate != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
			}

			if (toDate != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}