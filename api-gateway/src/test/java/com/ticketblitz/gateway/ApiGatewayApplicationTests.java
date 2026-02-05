package com.ticketblitz.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
		"spring.cloud.gateway.enabled=true"
})
class ApiGatewayApplicationTests {

	@Test
	void contextLoads() {
		// Test passes if context loads successfully
	}

}
