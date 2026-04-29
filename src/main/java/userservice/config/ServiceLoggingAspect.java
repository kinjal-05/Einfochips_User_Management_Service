package userservice.config;

import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Aspect for centralized logging of all service layer method executions.
 *
 * Responsibilities: 1. Log method entry with input arguments 2. Measure
 * execution time of service methods 3. Log method exit with result 4. Capture
 * and log exceptions for debugging
 *
 */
@Aspect
@Component
@Slf4j
public class ServiceLoggingAspect {

	/**
	 * Pointcut definition: Targets all methods inside service layer packages.
	 *
	 * Adjust "com.yourpackage" to your actual base package (e.g. userservice)
	 *
	 * This ensures all business logic methods are automatically logged.
	 */
	@Pointcut("within(com.yourpackage..service..*)")
	public void serviceMethods() {
	}

	/**
	 * Around advice: Intercepts service method execution and wraps logging around
	 * it.
	 *
	 * Flow: 1. Capture method name and arguments 2. Log method start 3. Execute
	 * actual business logic 4. Log execution time + result 5. Handle and log
	 * exceptions if any occur
	 */
	@Around("serviceMethods()")
	public Object logService(ProceedingJoinPoint joinPoint) throws Throwable {

		String methodName = joinPoint.getSignature().getName();
		Object[] args = joinPoint.getArgs();

		log.info("{} STARTED --> {}", methodName, Arrays.toString(args));

		long start = System.currentTimeMillis();

		try {
			Object result = joinPoint.proceed();

			log.info("{} COMPLETED --> {} | TIME={}ms | RESULT={}", methodName, Arrays.toString(args),
					(System.currentTimeMillis() - start), result);

			return result;

		} catch (Exception ex) {
			log.error("{} FAILED --> {} | ERROR={}", methodName, Arrays.toString(args), ex.getMessage(), ex);

			throw ex;
		}
	}
}
