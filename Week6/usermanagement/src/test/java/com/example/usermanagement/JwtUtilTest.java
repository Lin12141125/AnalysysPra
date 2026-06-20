package com.example.usermanagement;

import com.example.usermanagement.security.JwtUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JwtUtilTest {
    private JwtUtil jwtUtil;
	private static final String SECRET = "mySecretKeyForJWT12345678901234567890";
    private static final long EXPIRATION = 7200000L; // 2小时

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil();
        // 通过反射注入secret和expiration字段
        Field secretField = JwtUtil.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(jwtUtil, SECRET);

        Field expirationField = JwtUtil.class.getDeclaredField("expiration");
        expirationField.setAccessible(true);
        expirationField.set(jwtUtil, EXPIRATION);
    }

    // 测试生成Token并解析
	@Test
    @DisplayName("生成Token并正确解析")
	void tokenShouldBeGeneratedParsedCorrectly() throws Exception {
		String token = jwtUtil.generateToken(1, "alice", "ROLE_USER");

		assertTrue(jwtUtil.validateToken(token));
		assertEquals("alice", jwtUtil.getUsernameFromToken(token)); // 解析并断言用户名
		assertEquals(1, jwtUtil.getUserIdFromToken(token)); // 用户id
		assertEquals("ROLE_USER", jwtUtil.getRolesFromToken(token)); // 角色
		assertFalse(jwtUtil.isTokenExpired(token)); 
	}

    @Test
    @DisplayName("验证有效Token：应为true")
    void validateTokenShouldBeTrueForValidToken() {
        String token = jwtUtil.generateToken(1, "alice", "ROLE_USER");
        assertTrue(jwtUtil.validateToken(token));
    }

	@Test
    @DisplayName("过期Token验证失败")
	void expiredTokenShouldFailValidation() throws Exception {
        // 伪造一个已过期的token，通过反射修改expiration为负值
        Field expirationField = JwtUtil.class.getDeclaredField("expiration");
        expirationField.setAccessible(true);
        expirationField.set(jwtUtil, -60000L); // 设置过期时间为负值

		String token = jwtUtil.generateToken(2, "bob", "ROLE_ADMIN");

        // 回复原值，避免影响其他测试
        expirationField.set(jwtUtil, EXPIRATION);
        // 验证过期Token无效
		assertFalse(jwtUtil.validateToken(token));
	}

}
