package com.example.usermanagement;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"JWT_SECRET=test-jwt-secret-please-change-in-production-123456"
})
class UsermanagementApplicationTests {

	@Test
	void contextLoads() {
	}

}
