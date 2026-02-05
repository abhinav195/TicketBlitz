package com.ticketblitz.event.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLoggerTest {

    private AuditLogger auditLogger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        auditLogger = new AuditLogger();

        // Capture logs
        Logger logger = (Logger) LoggerFactory.getLogger("audit-logger");
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @Test
    @DisplayName("log: Should log audit entry with all details")
    void log_Success() {
        MDC.put("traceId", "trace-123");

        auditLogger.log("user-1", "CREATE_EVENT", "Event created", 1000000L, true);

        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertThat(logEvent.getMessage()).contains("trace-123");
        assertThat(logEvent.getMessage()).contains("user-1");
        assertThat(logEvent.getMessage()).contains("CREATE_EVENT");
        assertThat(logEvent.getMessage()).contains("Success");

        MDC.clear();
    }

    @Test
    @DisplayName("log: Should use N/A when traceId is not available")
    void log_NoTraceId() {
        MDC.clear();

        auditLogger.log("user-2", "SEARCH", "Search executed", 500000L, false);

        ILoggingEvent logEvent = listAppender.list.get(0);
        assertThat(logEvent.getMessage()).contains("N/A");
        assertThat(logEvent.getMessage()).contains("Fail");
    }
}
