package com.codesync.website;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"eureka.client.enabled=false",
		"spring.cloud.compatibility-verifier.enabled=false"
})
class WebsiteControllerApplicationTests {

	@Test
	void contextLoads() {
	}

}
