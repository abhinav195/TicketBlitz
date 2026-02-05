package com.ticketblitz.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

@SpringBootTest
@TestPropertySource(properties = {
		// Kafka Configuration
		"spring.kafka.bootstrap-servers=localhost:9092",
		"kafka.topics.payment-updates=payment.updates",
		"kafka.topics.recommendation-request=recommendation.request",
		"kafka.topics.email-dispatch=notification.email.dispatch",

		// Feign Client URLs
		"user-service.url=http://localhost:8085",
		"booking-service.url=http://localhost:8083",

		// Mail Configuration (Required for context to load)
		"spring.mail.host=localhost",
		"spring.mail.port=2525",
		"spring.mail.username=test",
		"spring.mail.password=test",

		// DISABLE ACTUATOR HEALTH CHECKS (Fix for mailHealthContributor error)
		"management.health.mail.enabled=false",
		"management.endpoints.web.exposure.include=health,info",

		// Disable Observability for tests
		"management.tracing.enabled=false",
		"management.metrics.export.prometheus.enabled=false"
})
class NotificationServiceApplicationTests {

	@MockitoBean
	private KafkaTemplate<String, Object> kafkaTemplate;

	@MockitoBean
	private JavaMailSender javaMailSender;

	@Test
	@DisplayName("Spring Context should load successfully")
	void contextLoads() {
		assertThat(kafkaTemplate).isNotNull();
		assertThat(javaMailSender).isNotNull();
	}

	@Test
	@DisplayName("NotificationServiceApplication class should be instantiable")
	void applicationClassShouldBeInstantiable() {
		NotificationServiceApplication application = new NotificationServiceApplication();
		assertThat(application).isNotNull();
	}

	@Test
	@DisplayName("Main method should call SpringApplication.run")
	void mainMethodShouldCallSpringApplicationRun() {
		try (MockedStatic<SpringApplication> springApplicationMock = mockStatic(SpringApplication.class)) {
			NotificationServiceApplication.main(new String[]{});

			springApplicationMock.verify(() ->
					SpringApplication.run(eq(NotificationServiceApplication.class), any(String[].class))
			);
		}
	}

	@Test
	@DisplayName("Application should have @SpringBootApplication annotation")
	void shouldHaveSpringBootApplicationAnnotation() {
		assertThat(NotificationServiceApplication.class.isAnnotationPresent(
				org.springframework.boot.autoconfigure.SpringBootApplication.class
		)).isTrue();
	}

	@Test
	@DisplayName("Application should have @EnableFeignClients annotation")
	void shouldHaveEnableFeignClientsAnnotation() {
		assertThat(NotificationServiceApplication.class.isAnnotationPresent(
				org.springframework.cloud.openfeign.EnableFeignClients.class
		)).isTrue();
	}
}
