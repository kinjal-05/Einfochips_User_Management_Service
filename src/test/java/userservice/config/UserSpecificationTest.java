package userservice.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import userservice.enums.Role;
import userservice.models.User;

/**
 * Unit test class for {@code UserSpecification}.
 *
 * <p>
 * This test suite validates the dynamic filtering logic implemented using
 * Spring Data JPA {@link Specification}. It ensures that the correct
 * {@link Predicate}s are constructed based on various combinations of input
 * filter parameters.
 *
 * <p>
 * <b>Testing Strategy:</b>
 * <ul>
 * <li>Uses {@link org.mockito.Mockito} to mock JPA Criteria API components:
 * {@link Root}, {@link CriteriaQuery}, and {@link CriteriaBuilder}</li>
 *
 * <li>Verifies interaction with {@link CriteriaBuilder} methods such as:
 * <ul>
 * <li>{@code like()} for email filtering</li>
 * <li>{@code equal()} for role and audit fields</li>
 * <li>{@code greaterThanOrEqualTo()} and {@code lessThanOrEqualTo()} for date
 * ranges</li>
 * <li>{@code isFalse()} for soft delete filtering</li>
 * </ul>
 * </li>
 *
 * <li>Ensures predicates are conditionally added only when corresponding filter
 * values are non-null and valid.</li>
 * </ul>
 *
 * <p>
 * <b>Key Test Scenarios:</b>
 * <ul>
 * <li><b>Soft Delete Filter:</b> Always verifies that {@code isDeleted = false}
 * predicate is applied.</li>
 *
 * <li><b>Email Filter:</b>
 * <ul>
 * <li>Ignored when null, empty, or blank</li>
 * <li>Applies case-insensitive {@code LIKE} query when valid</li>
 * <li>Ensures trimming of whitespace</li>
 * </ul>
 * </li>
 *
 * <li><b>Role Filter:</b> Applied only when role is non-null using
 * {@code EQUAL} predicate.</li>
 *
 * <li><b>Audit Filters:</b>
 * <ul>
 * <li>{@code createdById} and {@code updatedById} use {@code EQUAL}</li>
 * <li>Ignored when null</li>
 * </ul>
 * </li>
 *
 * <li><b>Date Range Filters:</b>
 * <ul>
 * <li>{@code fromDate} → {@code greaterThanOrEqualTo}</li>
 * <li>{@code toDate} → {@code lessThanOrEqualTo}</li>
 * <li>Ignored when null</li>
 * </ul>
 * </li>
 *
 * <li><b>Combination Scenarios:</b>
 * <ul>
 * <li>No filters → only soft delete predicate applied</li>
 * <li>All filters → all predicates combined using {@code AND}</li>
 * <li>Partial filters → only relevant predicates included</li>
 * </ul>
 * </li>
 * </ul>
 *
 * <p>
 * <b>Design Notes:</b>
 * <ul>
 * <li>Ensures correctness of dynamic query generation logic</li>
 * <li>Prevents unnecessary predicate creation for invalid inputs</li>
 * <li>Improves maintainability and reliability of filtering functionality</li>
 * </ul>
 *
 * <p>
 * <b>Annotations Used:</b>
 * <ul>
 * <li>{@code @ExtendWith(MockitoExtension.class)} for Mockito support</li>
 * <li>{@code @ActiveProfiles("test")} to activate test configuration</li>
 * <li>{@code @Nested} and {@code @DisplayName} for structured and readable test
 * cases</li>
 * </ul>
 */
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class UserSpecificationTest {

	@Mock
	private Root<User> root;
	@Mock
	private CriteriaQuery<?> query;
	@Mock
	private CriteriaBuilder cb;

	@Mock
	private Predicate isDeletedPredicate;
	@Mock
	private Predicate emailPredicate;
	@Mock
	private Predicate rolePredicate;
	@Mock
	private Predicate createdByIdPredicate;
	@Mock
	private Predicate updatedByIdPredicate;
	@Mock
	private Predicate fromDatePredicate;
	@Mock
	private Predicate toDatePredicate;
	@Mock
	private Predicate combinedAndPredicate;

	// Correctly-typed Path mocks — must match what each CriteriaBuilder method
	// expects
	@Mock
	private Path<Boolean> isDeletedPath; // isFalse(Expression<Boolean>)
	@Mock
	private Path<String> emailPath; // lower(Expression<String>)
	@Mock
	private Expression<String> lowerEmailExpr;
	@Mock
	private Path<Object> rolePath;
	@Mock
	private Path<Object> createdByIdPath;
	@Mock
	private Path<Object> updatedByIdPath;
	@Mock
	private Path<LocalDateTime> createdAtPath; // greaterThanOrEqualTo / lessThanOrEqualTo

	private static final LocalDateTime FROM = LocalDateTime.of(2024, 1, 1, 0, 0);
	private static final LocalDateTime TO = LocalDateTime.of(2024, 12, 31, 23, 59);

	/**
	 * Initializes common mock behavior before each test execution.
	 *
	 * <p>
	 * This setup method prepares reusable stubs for predicates that are always
	 * expected in specification-based query tests.
	 *
	 * <p>
	 * Configured mocks:
	 * <ul>
	 * <li>Maps {@code root.get("isDeleted")} to mocked boolean path</li>
	 * <li>Mocks {@code cb.isFalse(...)} for filtering active records</li>
	 * <li>Mocks {@code cb.and(...)} for combining predicates</li>
	 * </ul>
	 *
	 * <p>
	 * This ensures each test starts with consistent query-building behavior while
	 * reducing duplicated setup code.
	 *
	 * <p>
	 * Typical use case: Soft-delete implementations where every query must enforce
	 * {@code isDeleted = false}.
	 */
	@BeforeEach
	void stubAlwaysOnPredicates() {
		when(root.<Boolean>get("isDeleted")).thenReturn(isDeletedPath);
		when(cb.isFalse(isDeletedPath)).thenReturn(isDeletedPredicate);
		when(cb.and(any(Predicate[].class))).thenReturn(combinedAndPredicate);
	}

	/**
	 * Builds and evaluates a dynamic user search specification for testing.
	 *
	 * <p>
	 * This helper method invokes {@code UserSpecification.filterUsers(...)} using
	 * the supplied filter parameters and converts the resulting
	 * {@link Specification} into a JPA {@link Predicate}.
	 *
	 * <p>
	 * Typically used in unit tests to verify that the generated query conditions
	 * are correct for different combinations of inputs.
	 *
	 * <p>
	 * Supported filter inputs:
	 * <ul>
	 * <li>Email</li>
	 * <li>User role</li>
	 * <li>Created by user ID</li>
	 * <li>Updated by user ID</li>
	 * <li>Start date range</li>
	 * <li>End date range</li>
	 * </ul>
	 *
	 * @param email       optional email filter
	 * @param role        optional role filter
	 * @param createdById optional creator user ID filter
	 * @param updatedById optional updater user ID filter
	 * @param from        optional start date/time filter
	 * @param to          optional end date/time filter
	 * @return generated predicate based on supplied filters
	 */
	private Predicate evaluate(String email, Role role, Long createdById, Long updatedById, LocalDateTime from,
			LocalDateTime to) {
		Specification<User> spec = UserSpecification.filterUsers(email, role, createdById, updatedById, from, to);
		return spec.toPredicate(root, query, cb);
	}

	/**
	 * Test suite validating mandatory soft-delete predicate behavior.
	 *
	 * <p>
	 * Ensures that all generated specifications automatically include a condition
	 * that excludes logically deleted users ({@code isDeleted = false}), regardless
	 * of any optional filters.
	 */
	@Nested
	@DisplayName("isDeleted predicate")
	class IsDeletedPredicateTests {

		/**
		 * Verifies that the soft-delete predicate is always included even when no
		 * search filters are supplied.
		 *
		 * <p>
		 * Expected behavior:
		 * <ul>
		 * <li>{@code cb.isFalse(isDeletedPath)} is invoked</li>
		 * <li>Combined predicate is returned</li>
		 * </ul>
		 */
		@Test
		@DisplayName("always added regardless of other filters")
		void alwaysAdded() {
			Predicate result = evaluate(null, null, null, null, null, null);
			verify(cb).isFalse(isDeletedPath);
			assertThat(result).isEqualTo(combinedAndPredicate);
		}
	}

	/**
	 * Test suite validating email-based dynamic filtering behavior.
	 *
	 * <p>
	 * Ensures email search is:
	 * <ul>
	 * <li>Optional</li>
	 * <li>Case-insensitive</li>
	 * <li>Whitespace-trimmed</li>
	 * <li>Implemented using LIKE matching</li>
	 * </ul>
	 */
	@Nested
	@DisplayName("email filter")
	class EmailFilterTests {

		/**
		 * Prepares common mock behavior for email predicate generation.
		 *
		 * @param trimmedLower normalized lowercase email fragment
		 */
		private void stubEmailPath(String trimmedLower) {
			when(root.<String>get("email")).thenReturn(emailPath);
			when(cb.lower(emailPath)).thenReturn(lowerEmailExpr);
			when(cb.like(lowerEmailExpr, "%" + trimmedLower + "%")).thenReturn(emailPredicate);
		}

		/**
		 * Verifies that null, empty, or blank email values do not create unnecessary
		 * LIKE predicates.
		 *
		 * @param email blank or null email input
		 */
		@ParameterizedTest(name = "{index} — [{0}]")
		@DisplayName("blank/null email — skipped")
		@NullAndEmptySource
		@ValueSource(strings = { "   " })
		void blankOrNullEmail_skipped(String email) {
			evaluate(email, null, null, null, null, null);
			verify(cb, never()).like(any(), anyString());
		}

		/**
		 * Verifies that valid email input creates a case-insensitive LIKE predicate.
		 */
		@Test
		@DisplayName("valid email — case-insensitive LIKE predicate added")
		void validEmail_predicateAdded() {
			stubEmailPath("kinjal");
			Predicate result = evaluate("Kinjal", null, null, null, null, null);
			verify(cb).lower(emailPath);
			verify(cb).like(lowerEmailExpr, "%kinjal%");
			assertThat(result).isEqualTo(combinedAndPredicate);
		}

		/**
		 * Verifies that surrounding whitespace is removed before building the LIKE
		 * predicate.
		 */
		@Test
		@DisplayName("email with surrounding whitespace — trimmed before LIKE")
		void emailWithWhitespace_trimmed() {
			stubEmailPath("kinjal");
			evaluate("  kinjal  ", null, null, null, null, null);
			verify(cb).like(lowerEmailExpr, "%kinjal%");
		}
	}

	/**
	 * Test suite validating dynamic role-based filtering behavior.
	 *
	 * <p>
	 * Ensures that role criteria are only applied when a valid non-null role is
	 * supplied.
	 */
	@Nested
	@DisplayName("role filter")
	class RoleFilterTests {

		/**
		 * Verifies that a null role input does not generate an unnecessary equality
		 * predicate.
		 */
		@Test
		@DisplayName("null role — skipped")
		void nullRole_skipped() {
			evaluate(null, null, null, null, null, null);
			verify(cb, never()).equal(any(Expression.class), any(Role.class));
		}

		/**
		 * Verifies that a non-null role generates an equality predicate.
		 *
		 * <p>
		 * Expected query condition: {@code role = ROLE_ADMIN}
		 */
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

	/**
	 * Test suite validating createdById filtering behavior.
	 *
	 * <p>
	 * Ensures creator user filtering is applied only when a valid identifier is
	 * provided.
	 */
	@Nested
	@DisplayName("createdById filter")
	class CreatedByIdFilterTests {

		/**
		 * Verifies that null createdById input is ignored.
		 */
		@Test
		@DisplayName("null createdById — skipped")
		void nullCreatedById_skipped() {
			evaluate(null, null, null, null, null, null);
			verify(cb, never()).equal(any(Expression.class), eq(10L));
		}

		/**
		 * Verifies that a non-null createdById generates an equality predicate.
		 *
		 * <p>
		 * Expected query condition: {@code createdById = 10}
		 */
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

	/**
	 * Test suite validating updatedById filtering behavior.
	 *
	 * <p>
	 * Ensures updater user filtering is applied only when a valid identifier is
	 * supplied.
	 */
	@Nested
	@DisplayName("updatedById filter")
	class UpdatedByIdFilterTests {

		/**
		 * Verifies that null updatedById input is ignored.
		 */
		@Test
		@DisplayName("null updatedById — skipped")
		void nullUpdatedById_skipped() {
			evaluate(null, null, null, null, null, null);
			verify(cb, never()).equal(any(Expression.class), eq(99L));
		}

		/**
		 * Verifies that a non-null updatedById generates an equality predicate.
		 *
		 * <p>
		 * Expected query condition: {@code updatedById = 99}
		 */
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

	/**
	 * Test suite validating lower-bound date filtering behavior.
	 *
	 * <p>
	 * Ensures that the {@code fromDate} parameter is applied as a "created at
	 * greater than or equal to" condition when provided.
	 */
	@Nested
	@DisplayName("fromDate filter")
	class FromDateFilterTests {

		/**
		 * Verifies that a null fromDate value does not generate a lower-bound date
		 * predicate.
		 */
		@Test
		@DisplayName("null fromDate — skipped")
		void nullFromDate_skipped() {
			evaluate(null, null, null, null, null, null);
			verify(cb, never()).greaterThanOrEqualTo(any(), any(LocalDateTime.class));
		}

		/**
		 * Verifies that a non-null fromDate creates a greater-than-or-equal date
		 * predicate.
		 *
		 * <p>
		 * Expected query condition: {@code createdAt >= FROM}
		 */
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

	/**
	 * Test suite validating upper-bound date filtering behavior.
	 *
	 * <p>
	 * Ensures that the {@code toDate} parameter is applied as a "created at less
	 * than or equal to" condition when provided.
	 */
	@Nested
	@DisplayName("toDate filter")
	class ToDateFilterTests {

		/**
		 * Verifies that a null toDate value does not generate an upper-bound date
		 * predicate.
		 */
		@Test
		@DisplayName("null toDate — skipped")
		void nullToDate_skipped() {
			evaluate(null, null, null, null, null, null);
			verify(cb, never()).lessThanOrEqualTo(any(), any(LocalDateTime.class));
		}

		/**
		 * Verifies that a non-null toDate creates a less-than-or-equal date predicate.
		 *
		 * <p>
		 * Expected query condition: {@code createdAt <= TO}
		 */
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

	/**
	 * Test suite validating combined specification scenarios where multiple
	 * optional filters interact together.
	 *
	 * <p>
	 * These tests ensure the final predicate composition remains correct for
	 * minimal, full, and partial filter combinations.
	 */
	@Nested
	@DisplayName("combination scenarios")
	class CombinationScenarioTests {

		/**
		 * Verifies behavior when no optional filters are supplied.
		 *
		 * <p>
		 * Expected outcome:
		 * <ul>
		 * <li>Only soft-delete predicate is applied</li>
		 * <li>Predicates are combined once using AND</li>
		 * <li>No optional filter predicates are created</li>
		 * </ul>
		 */
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

		/**
		 * Verifies behavior when every available filter is supplied.
		 *
		 * <p>
		 * Expected outcome:
		 * <ul>
		 * <li>Email LIKE predicate created</li>
		 * <li>Role equality predicate created</li>
		 * <li>createdById equality predicate created</li>
		 * <li>updatedById equality predicate created</li>
		 * <li>Date range predicates created</li>
		 * <li>All predicates combined using AND</li>
		 * </ul>
		 */
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

		/**
		 * Verifies behavior when only date range filters are supplied.
		 *
		 * <p>
		 * Expected outcome:
		 * <ul>
		 * <li>Lower and upper date predicates are generated</li>
		 * <li>Soft-delete predicate remains active</li>
		 * <li>No email predicate is created</li>
		 * </ul>
		 */
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