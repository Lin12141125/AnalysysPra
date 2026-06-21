package com.example.usermanagement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI userManagementOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("用户管理系统 API")
                        .description("用户管理、认证授权接口文档")
                        .version("1.6.0")
                        .contact(new Contact()
                                .name("Yingzi Li")
                                .email("2546156509@qq.com")));
    }
}