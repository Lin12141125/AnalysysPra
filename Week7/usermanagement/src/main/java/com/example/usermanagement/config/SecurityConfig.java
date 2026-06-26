package com.example.usermanagement.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.usermanagement.security.JwtAccessDeniedHandler;
import com.example.usermanagement.security.JwtAuthenticationEntryPoint;
import com.example.usermanagement.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // 启用 @PreAuthorize 注解: SpringSecurity提供的一种权限控制注解，用于在方法执行前进行权限校验
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // 禁用CSRF保护，因为我们使用JWT进行认证，不需要CSRF保护(无状态的API)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 设置Session管理为无状态，因为我们使用JWT进行认证，不需要Session
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll() // 允许登录和注册接口无需认证
                .requestMatchers(
                    "/doc.html", // Knife4j文档入口
                    "/v3/api-docs/**", // Swagger文档JSON端点
                    "/swagger-ui/**",
                    "/swagger-resources/**", // Swagger资源配置端点
                    "/webjars/**" // Swagger前端静态资源端点
                ).permitAll() // 放行Knife4j和OpenAPI文档路径，允许Swagger相关接口无需认证
                .requestMatchers(HttpMethod.POST, "/api/users", "/api/users/*/avatar").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/users/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/users/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/users", "/api/users/*", "/api/users/page", "/api/users/*/avatar").hasAnyRole("ADMIN", "USER")
                .anyRequest().authenticated() // 其他所有请求都需要认证
            ) // 添加自定义异常处理，返回 JSON 而不是重定向/HTML
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // 在UsernamePassword

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

}
