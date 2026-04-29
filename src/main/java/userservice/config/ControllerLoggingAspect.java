package userservice.config;

import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Aspect for centralized logging of all REST controller executions.
 *
 * Responsibilities: 1. Log incoming HTTP request details (method, URI,
 * arguments) 2. Measure execution time of controller methods 3. Log response
 * returned by controller 4. Capture and log exceptions globally at controller
 * level
 *
 */
@Aspect
@Component
@Slf4j
public class ControllerLoggingAspect {

	/**
	 * Pointcut definition: Targets all classes annotated with @RestController.
	 *
	 * This ensures that every REST API endpoint is intercepted without manually
	 * adding logging in each controller.
	 */
	@Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
	public void restControllerMethods() {
	}

	/**
	 * Around advice: Executes before and after controller method execution.
	 *
	 * Flow: 1. Capture request metadata (method, URI) 2. Log input arguments 3.
	 * Execute actual controller method 4. Log response + execution time 5. Handle
	 * and log exceptions if any occur
	 */
	@Around("restControllerMethods()")
	public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {

		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
				.getRequest();

		String path = request.getRequestURI();
		String method = request.getMethod();

		log.info("CONTROLLER START --> {} {}", method, path);
		log.info("ARGS --> {}", Arrays.toString(joinPoint.getArgs()));

		long start = System.currentTimeMillis();

		try {
			Object result = joinPoint.proceed();
			log.info("CONTROLLER END --> {} {} | TIME={}ms | RESPONSE={}", method, path,
					(System.currentTimeMillis() - start), result);
			return result;

		} catch (Exception ex) {
			log.error("CONTROLLER ERROR --> {} {} | ERROR={}", method, path, ex.getMessage(), ex);

			throw ex;
		}
	}
}