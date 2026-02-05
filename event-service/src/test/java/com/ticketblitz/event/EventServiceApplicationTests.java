package com.ticketblitz.event;

import com.ticketblitz.event.service.MinioService;
import io.minio.MinioClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:testdb",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.flyway.enabled=false",
		"spring.kafka.bootstrap-servers=localhost:9092",
		"spring.kafka.producer.bootstrap-servers=localhost:9092",
		"minio.url=http://localhost:9000",
		"minio.access-key=minioadmin",
		"minio.secret-key=minioadmin",
		"minio.bucket-name=test-bucket",
		"jwt.secret=test-secret-key-for-testing-purposes-only-minimum-256-bits-long-secret"
})
class EventServiceApplicationTests {

	@MockitoBean
	private MinioClient minioClient;

	@MockitoBean
	private MinioService minioService;

	@MockitoBean
	private KafkaTemplate<String, Object> kafkaTemplate;

	@Test
	@DisplayName("Spring Context should load successfully")
	void contextLoads() {
		assertThat(minioClient).isNotNull();
		assertThat(minioService).isNotNull();
		assertThat(kafkaTemplate).isNotNull();
	}

	@Test
	@DisplayName("EventServiceApplication class should be instantiable")
	void applicationClassShouldBeInstantiable() {
		EventServiceApplication application = new EventServiceApplication();
		assertThat(application).isNotNull();
	}

	@Test
	@DisplayName("Main method should call SpringApplication.run")
	void mainMethodShouldCallSpringApplicationRun() {
		try (MockedStatic<SpringApplication> springApplicationMock = mockStatic(SpringApplication.class)) {
			// Call the main method
			EventServiceApplication.main(new String[]{});

			// Verify SpringApplication.run was called
			springApplicationMock.verify(() ->
					SpringApplication.run(eq(EventServiceApplication.class), any(String[].class))
			);
		}
	}
}
