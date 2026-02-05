package com.ticketblitz.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "stripe.api-key=sk_test_fake_key_for_testing",
        "stripe.currency=inr"
})
class PaymentServiceApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("Spring Context: Should load successfully")
    void contextShouldLoadSuccessfully() {
        assertThat(applicationContext).isNotNull();
        assertThat(applicationContext.getBeanDefinitionCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("PaymentServiceApplication: Should be instantiable")
    void applicationClassShouldBeInstantiable() {
        PaymentServiceApplication application = new PaymentServiceApplication();
        assertThat(application).isNotNull();
    }

    @Test
    @DisplayName("Main method: Should call SpringApplication.run")
    void mainMethodShouldCallSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApplicationMock = mockStatic(SpringApplication.class)) {
            // Act
            PaymentServiceApplication.main(new String[]{});

            // Assert
            springApplicationMock.verify(() ->
                    SpringApplication.run(eq(PaymentServiceApplication.class), any(String[].class))
            );
        }
    }

    @Test
    @DisplayName("Main method: Should pass command line arguments")
    void mainMethodShouldPassCommandLineArguments() {
        try (MockedStatic<SpringApplication> springApplicationMock = mockStatic(SpringApplication.class)) {
            // Arrange
            String[] args = {"--server.port=8085", "--spring.profiles.active=test"};

            // Act
            PaymentServiceApplication.main(args);

            // Assert
            springApplicationMock.verify(() ->
                    SpringApplication.run(eq(PaymentServiceApplication.class), eq(args))
            );
        }
    }

    @Test
    @DisplayName("PaymentServiceApplication: Should have @SpringBootApplication annotation")
    void shouldHaveSpringBootApplicationAnnotation() {
        assertThat(PaymentServiceApplication.class.isAnnotationPresent(SpringBootApplication.class)).isTrue();
    }

    @Test
    @DisplayName("Spring Context: Should contain PaymentService bean")
    void contextShouldContainPaymentServiceBean() {
        assertThat(applicationContext.containsBean("paymentService")).isTrue();
    }

    @Test
    @DisplayName("Spring Context: Should contain PaymentRepository bean")
    void contextShouldContainPaymentRepositoryBean() {
        assertThat(applicationContext.containsBean("paymentRepository")).isTrue();
    }

    @Test
    @DisplayName("Spring Context: Should contain PaymentConsumer bean")
    void contextShouldContainPaymentConsumerBean() {
        assertThat(applicationContext.containsBean("paymentConsumer")).isTrue();
    }

    @Test
    @DisplayName("Spring Context: Should contain PaymentProducer bean")
    void contextShouldContainPaymentProducerBean() {
        assertThat(applicationContext.containsBean("paymentProducer")).isTrue();
    }

    @Test
    @DisplayName("Spring Context: Should contain KafkaTemplate bean")
    void contextShouldContainKafkaTemplateBean() {
        assertThat(applicationContext.containsBean("kafkaTemplate")).isTrue();
    }

    @Test
    @DisplayName("Spring Context: Should contain ObjectMapper bean")
    void contextShouldContainObjectMapperBean() {
        assertThat(applicationContext.containsBean("objectMapper")).isTrue();
    }

    @Test
    @DisplayName("Application: Should have main method")
    void shouldHaveMainMethod() throws NoSuchMethodException {
        assertThat(PaymentServiceApplication.class.getMethod("main", String[].class)).isNotNull();
    }

    @Test
    @DisplayName("Application: Main method should be public static void")
    void mainMethodShouldBePublicStaticVoid() throws NoSuchMethodException {
        var mainMethod = PaymentServiceApplication.class.getMethod("main", String[].class);

        assertThat(java.lang.reflect.Modifier.isPublic(mainMethod.getModifiers())).isTrue();
        assertThat(java.lang.reflect.Modifier.isStatic(mainMethod.getModifiers())).isTrue();
        assertThat(mainMethod.getReturnType()).isEqualTo(void.class);
    }
}
