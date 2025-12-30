package com.ticketblitz.event.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class AuditLogger {
    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("audit-logger");

    public void log(String userId, String action, String details, long durationNs, boolean success) {
        String traceId = MDC.get("traceId"); // Will be populated by Micrometer/Sleuth later
        if (traceId == null) traceId = "N/A";

        // Format: [TRACE_ID] [TIME] [USER_ID] Action: {ActionName} | Duration: {ns} | Result: {Success/Fail}
        String logEntry = String.format("[%s] [%s] [%s] Action: %s | Details: %s | Duration: %d ns | Result: %s",
                traceId,
                java.time.Instant.now(),
                userId,
                action,
                details,
                durationNs,
                success ? "Success" : "Fail"
        );

        AUDIT_LOG.info(logEntry);
    }
}
