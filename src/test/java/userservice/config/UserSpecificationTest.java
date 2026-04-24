package userservice.config;

import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import userservice.enums.Role;
import userservice.models.User;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test class for {@code UserSpecification}.
 *
 * <p>This test suite validates the dynamic filtering logic implemented using
 * Spring Data JPA {@link Specification}. It ensures that the correct
 * {@link Predicate}s are constructed based on various combinations of
 * input filter parameters.
 *
 * <p><b>Testing Strategy:</b>
 * <ul>
 *   <li>Uses {@link org.mockito.Mockito} to mock JPA Criteria API components:
 *       {@link Root}, {@link CriteriaQuery}, and {@link CriteriaBuilder}</li>
 *
 *   <li>Verifies interaction with {@link CriteriaBuilder} methods such as:
 *       <ul>
 *         <li>{@code like()} for email filtering</li>
 *         <li>{@code equal()} for role and audit fields</li>
 *         <li>{@code greaterThanOrEqualTo()} and {@code lessThanOrEqualTo()} for date ranges</li>
 *         <li>{@code isFalse()} for soft delete filtering</li>
 *       </ul>
 *   </li>
 *
 *   <li>Ensures predicates are conditionally added only when corresponding
 *       filter values are non-null and valid.</li>
 * </ul>
 *
 * <p><b>Key Test Scenarios:</b>
 * <ul>
 *   <li><b>Soft Delete Filter:</b>
 *       Always verifies that {@code isDeleted = false} predicate is applied.</li>
 *
 *   <li><b>Email Filter:</b>
 *       <ul>
 *         <li>Ignored when null, empty, or blank</li>
 *         <li>Applies case-insensitive {@code LIKE} query when valid</li>
 *         <li>Ensures trimming of whitespace</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>Role Filter:</b>
 *       Applied only when role is non-null using {@code EQUAL} predicate.</li>
 *
 *   <li><b>Audit Filters:</b>
 *       <ul>
 *         <li>{@code createdById} and {@code updatedById} use {@code EQUAL}</li>
 *         <li>Ignored when null</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>Date Range Filters:</b>
 *       <ul>
 *         <li>{@code fromDate} → {@code greaterThanOrEqualTo}</li>
 *         <li>{@code toDate} → {@code lessThanOrEqualTo}</li>
 *         <li>Ignored when null</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>Combination Scenarios:</b>
 *       <ul>
 *         <li>No filters → only soft delete predicate applied</li>
 *         <li>All filters → all predicates combined using {@code AND}</li>
 *         <li>Partial filters → only relevant predicates included</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <p><b>Design Notes:</b>
 * <ul>
 *   <li>Ensures correctness of dynamic query generation logic</li>
 *   <li>Prevents unnecessary predicate creation for invalid inputs</li>
 *   <li>Improves maintainability and reliability of filtering functionality</li>
 * </ul>
 *
 * <p><b>Annotations Used:</b>
 * <ul>
 *   <li>{@code @ExtendWith(MockitoExtension.class)} for Mockito support</li>
 *   <li>{@code @ActiveProfiles("test")} to activate test configuration</li>
 *   <li>{@code @Nested} and {@code @DisplayName} for structured and readable test cases</li>
 * </ul>
 */
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class UserSpecificationTest {

	@Mock private Root<User>        root;
	@Mock private CriteriaQuery<?>  query;
	@Mock private CriteriaBuilder   cb;

	@Mock private Predicate isDeletedPredicate;
	@Mock private Predicate emailPredicate;
	@Mock private Predicate rolePredicate;
	@Mock private Predicate createdByIdPredicate;
	@Mock private Predicate updatedByIdPredicate;
	@Mock private Predicate fromDatePredicate;
	@Mock private Predicate toDatePredicate;
	@Mock private Predicate combinedAndPredicate;

	// Correctly-typed Path mocks — must match what each CriteriaBuilder method expects
	@Mock private Path<Boolean>       isDeletedPath;   // isFalse(Expression<Boolean>)
	@Mock private Path<String>        emailPath;        // lower(Expression<String>)
	@Mock private Expression<String>  lowerEmailExpr;
	@Mock private Path<Object>        rolePath;
	@Mock private Path<Object>        createdByIdPath;
	@Mock private Path<Object>        updatedByIdPath;
	@Mock private Path<LocalDateTime> createdAtPath;   // greaterThanOrEqualTo / lessThanOrEqualTo

	private static final LocalDateTime FROM = LocalDateTime.of(2024, 1, 1, 0, 0);
	private static final LocalDateTime TO   = LocalDateTime.of(2024, 12, 31, 23, 59);

	@BeforeEach
	void stubAlwaysOnPredicates() {
		when(root.<Boolean>get("isDeleted")).thenReturn(isDeletedPath);
		when(cb.isFalse(isDeletedPath)).thenReturn(isDeletedPredicate);
		when(cb.and(any(Predicate[].class))).thenReturn(combinedAndPredicate);
	}

	private Predicate evaluate(String email, Role role, Long createdById,
	                           Long updatedById, LocalDateTime from, LocalDateTime to) {
		Specification<User> spec = UserSpecification.filterUsers(
				email, role, createdById, updatedById, from, to);
		return spec.toPredicate(root, query, cb);
	}

	@Nested
	@DisplayName("isDeleted predicate")
	class IsDeletedPredicateTests {

		@Test
		@DisplayName("always added regardless of other filters")
		void alwaysAdded() {
			Predicate result = evaluate(null, null, null, null, null, null);
			verify(cb).isFalse(isDeletedPath);
			assertThat(result).isEqualTo(combinedAndPredicate);
		}
	}

	@Nested
	@DisplayName("email filter")
	class EmailFilterTests {

		private void stubEmailPath(String trimmedLower) {
			when(root.<String>get("email")).thenReturn(emailPath);
			when(cb.lower(emailPath)).thenReturn(lowerEmailExpr);
			when(cb.like(lowerEmailExpr, "%" + trimmedLower + "%")).thenReturn(emailPredicate);
		}

		@Test
		@DisplayName("null email — skipped")
		void nullEmail_skipped() {
			evaluate(null, null, null, null, null, null);
			verify(cb, never()).like(any(Expression.class), anyString());
		}

		@Test
		@DisplayName("empty string — skipped")
		void emptyEmail_skipped() {
			evaluate("", null, null, null, null, null);
			verify(cb, never()).like(any(Expression.class), anyString());
		}

		@Test
		@DisplayName("whitespace-only — skipped")
		void blankEmail_skipped() {
			evaluate("   ", null, null, null, null, null);
			verify(cb, never()).like(any(Expression.class), anyString());
		}

		@Test
		@DisplayName("valid email — case-insensitive LIKE predicate added")
		void validEmail_predicateAdded() {
			stubEmailPath("kinjal");
			Predicate result = evaluate("Kinjal", null, null, null, null, null);
			verify(cb).lower(emailPath);
			verify(cb).like(lowerEmailExpr, "%kinjal%");
			assertThat(result).isEqualTo(combinedAndPredicate);
		}

		@Test
		@DisplayName("email with surrounding whitespace — trimmed before LIKE")
		void emailWithWhitespace_trimmed() {
			stubEmailPath("kinjal");
			evaluate("  kinjal  ", null, null, null, null, null);
			verify(cb).like(lowerEmailExpr, "%kinjal%");
		}
	}

	@Nested
	@DisplayName("role filter")
	class RoleFilterTests {

		@Test
		@DisplayName("null role — skipped")
		void nullRole_skipped() {
			evaluate(null, null, null, null, null, null);
			verify(cb, never()).equal(any(Expression.class), any(Role.class));
		}

		@Test
		@DisplayName("non-null role — EQUAL predicate added")
		void nonNullRole_predicateAdded() {
			when(root.get("role")).thenReturn(rolePath);
			when(cb.equal(rolePath, Role.ROLE_ADMIN)).thenReturn(rolePredicate);
			Predicate result = evaluate(null, Role.ROLE_ADMIN, null, null, null, null);
			verify(cb).equal(rolePath, Role.ROLE_ADMIN);
			assertThat(result).isEqualTo(combinedAndPredicate);
		}
	}

	@Nested
	@DisplayName("createdById filter")
	class CreatedByIdFilterTests {

		@Test
		@DisplayName("null createdById — skipped")
		void nullCreatedById_skipped() {
			evaluate(null, null, null, null, null, null);
			verify(cb, never()).equal(any(Expression.class), eq(10L));
		}

		@Test
		@DisplayName("non-null createdById — EQUAL predicate added")
		void nonNullCreatedById_predicateAdded() {
			when(root.get("createdById")).thenReturn(createdByIdPath);
			when(cb.equal(createdByIdPath, 10L)).thenReturn(createdByIdPredicate);
			Predicate result = evaluate(null, null, 10L, null, null, null);
			verify(cb).equal(createdByIdPath, 10L);
			assertThat(result).isEqualTo(combinedAndPredicate);
		}
	}

	@Nested
	@DisplayName("updatedById filter")
	class UpdatedByIdFilterTests {

		@Test
		@DisplayName("null updatedById — skipped")
		void nullUpdatedById_skipped() {
			evaluate(null, null, null, null, null, null);
			verify(cb, never()).equal(any(Expression.class), eq(99L));
		}

		@Test
		@DisplayName("non-null updatedById — EQUAL predicate added")
		void nonNullUpdatedById_predicateAdded() {
			when(root.get("updatedById")).thenReturn(updatedByIdPath);
			when(cb.equal(updatedByIdPath, 99L)).thenReturn(updatedByIdPredicate);
			Predicate result = evaluate(null, null, null, 99L, null, null);
			verify(cb).equal(updatedByIdPath, 99L);
			assertThat(result).isEqualTo(combinedAndPredicate);
		}
	}

	@Nested
	@DisplayName("fromDate filter")
	class FromDateFilterTests {

		@Test
		@DisplayName("null fromDate — skipped")
		void nullFromDate_skipped() {
			evaluate(null, null, null, null, null, null);
			verify(cb, never()).greaterThanOrEqualTo(any(), any(LocalDateTime.class));
		}

		@Test
		@DisplayName("non-null fromDate — greaterThanOrEqualTo predicate added")
		void nonNullFromDate_predicateAdded() {
			when(root.<LocalDateTime>get("createdAt")).thenReturn(createdAtPath);
			when(cb.greaterThanOrEqualTo(createdAtPath, FROM)).thenReturn(fromDatePredicate);
			Predicate result = evaluate(null, null, null, null, FROM, null);
			verify(cb).greaterThanOrEqualTo(createdAtPath, FROM);
			assertThat(result).isEqualTo(combinedAndPredicate);
		}
	}

	@Nested
	@DisplayName("toDate filter")
	class ToDateFilterTests {

		@Test
		@DisplayName("null toDate — skipped")
		void nullToDate_skipped() {
			evaluate(null, null, null, null, null, null);
			verify(cb, never()).lessThanOrEqualTo(any(), any(LocalDateTime.class));
		}

		@Test
		@DisplayName("non-null toDate — lessThanOrEqualTo predicate added")
		void nonNullToDate_predicateAdded() {
			when(root.<LocalDateTime>get("createdAt")).thenReturn(createdAtPath);
			when(cb.lessThanOrEqualTo(createdAtPath, TO)).thenReturn(toDatePredicate);
			Predicate result = evaluate(null, null, null, null, null, TO);
			verify(cb).lessThanOrEqualTo(createdAtPath, TO);
			assertThat(result).isEqualTo(combinedAndPredicate);
		}
	}

	@Nested
	@DisplayName("combination scenarios")
	class CombinationScenarioTests {

		@Test
		@DisplayName("no filters — only isDeleted predicate, AND called once")
		void noFilters_onlyIsDeletedPredicate() {
			evaluate(null, null, null, null, null, null);
			verify(cb, times(1)).and(any(Predicate[].class));
			verify(cb, never()).like(any(Expression.class), anyString());
			verify(cb, never()).equal(any(Expression.class), any());
			verify(cb, never()).greaterThanOrEqualTo(any(), any(LocalDateTime.class));
			verify(cb, never()).lessThanOrEqualTo(any(), any(LocalDateTime.class));
		}

		@Test
		@DisplayName("all filters supplied — every predicate built and combined")
		void allFilters_allPredicatesBuilt() {
			when(root.<String>get("email")).thenReturn(emailPath);
			when(cb.lower(emailPath)).thenReturn(lowerEmailExpr);
			when(cb.like(lowerEmailExpr, "%kinjal%")).thenReturn(emailPredicate);

			when(root.get("role")).thenReturn(rolePath);
			when(cb.equal(rolePath, Role.ROLE_ADMIN)).thenReturn(rolePredicate);

			when(root.get("createdById")).thenReturn(createdByIdPath);
			when(cb.equal(createdByIdPath, 1L)).thenReturn(createdByIdPredicate);

			when(root.get("updatedById")).thenReturn(updatedByIdPath);
			when(cb.equal(updatedByIdPath, 2L)).thenReturn(updatedByIdPredicate);

			when(root.<LocalDateTime>get("createdAt")).thenReturn(createdAtPath);
			when(cb.greaterThanOrEqualTo(createdAtPath, FROM)).thenReturn(fromDatePredicate);
			when(cb.lessThanOrEqualTo(createdAtPath, TO)).thenReturn(toDatePredicate);

			Predicate result = evaluate("kinjal", Role.ROLE_ADMIN, 1L, 2L, FROM, TO);

			verify(cb).isFalse(isDeletedPath);
			verify(cb).like(lowerEmailExpr, "%kinjal%");
			verify(cb).equal(rolePath, Role.ROLE_ADMIN);
			verify(cb).equal(createdByIdPath, 1L);
			verify(cb).equal(updatedByIdPath, 2L);
			verify(cb).greaterThanOrEqualTo(createdAtPath, FROM);
			verify(cb).lessThanOrEqualTo(createdAtPath, TO);
			verify(cb, times(1)).and(any(Predicate[].class));
			assertThat(result).isEqualTo(combinedAndPredicate);
		}

		@Test
		@DisplayName("date range only — only date predicates alongside isDeleted")
		void dateRangeOnly() {
			when(root.<LocalDateTime>get("createdAt")).thenReturn(createdAtPath);
			when(cb.greaterThanOrEqualTo(createdAtPath, FROM)).thenReturn(fromDatePredicate);
			when(cb.lessThanOrEqualTo(createdAtPath, TO)).thenReturn(toDatePredicate);

			evaluate(null, null, null, null, FROM, TO);

			verify(cb).greaterThanOrEqualTo(createdAtPath, FROM);
			verify(cb).lessThanOrEqualTo(createdAtPath, TO);
			verify(cb, never()).like(any(Expression.class), anyString());
		}
	}
}