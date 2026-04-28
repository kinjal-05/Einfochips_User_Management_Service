package userservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
/**
 * Configuration class for enabling JPA auditing in the application.
 *
 * <p>
 * This configuration activates Spring Data JPA's auditing feature,
 * which automatically populates auditing-related fields such as
 * {@code createdBy}, {@code createdDate}, {@code lastModifiedBy},
 * and {@code lastModifiedDate} in entity classes.
 * </p>
 *
 * <p>
 * The {@code auditorAwareRef} attribute specifies the bean responsible
 * for providing the current authenticated user (auditor). In this case,
 * it refers to the {@code auditorAwareImpl} bean, which must implement
 * {@link org.springframework.data.domain.AuditorAware}.
 * </p>
 *
 * <p>
 * Ensure that:
 * <ul>
 *   <li>An {@code AuditorAware} implementation bean named
 *       {@code auditorAwareImpl} is defined in the application context.</li>
 *   <li>Entities that require auditing are annotated with
 *       {@code @EntityListeners(AuditingEntityListener.class)}.</li>
 *   <li>Auditing fields are properly annotated using
 *       {@code @CreatedBy}, {@code @CreatedDate},
 *       {@code @LastModifiedBy}, and {@code @LastModifiedDate}.</li>
 * </ul>
 * </p>
 *
 * <p>
 * This configuration is typically used in applications where tracking
 * of entity creation and modification metadata is required for
 * auditing, logging, or compliance purposes.
 * </p>
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
public class AuditConfig {
}
