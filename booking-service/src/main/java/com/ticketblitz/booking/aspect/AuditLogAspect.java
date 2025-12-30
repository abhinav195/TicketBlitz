package com.ticketblitz.booking.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
@Slf4j
public class AuditLogAspect {

    /**
     * Intercepts any method annotated with @AuditLog.
     * Measures execution time and logs success/failure.
     */
    @Around("@annotation(AuditLog)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.nanoTime();
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        String methodName = joinPoint.getSignature().getName();
        String status = "SUCCESS";

        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            status = "FAILED";
            throw ex;
        } finally {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            // Format: [TRACE_ID] Method: bookTicket | Time: 45ms | Status: SUCCESS
            log.info("[{}] Method: {} | Time: {}ms | Status: {}", traceId, methodName, durationMs, status);
        }
    }
}
