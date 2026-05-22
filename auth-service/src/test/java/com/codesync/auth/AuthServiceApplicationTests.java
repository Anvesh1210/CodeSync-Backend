package com.codesync.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AuthServiceApplicationTests {

	@Autowired
	private org.springframework.context.ApplicationContext context;

	@Test
	void contextLoads() {
		org.junit.jupiter.api.Assertions.assertNotNull(context, "The application context should not be null");
	}
}
