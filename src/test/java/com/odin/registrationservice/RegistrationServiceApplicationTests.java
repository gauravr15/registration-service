package com.odin.registrationservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = RegistrationServiceApplication.class, properties = {
		"spring.cloud.config.enabled=false",
		"eureka.client.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
class RegistrationServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
