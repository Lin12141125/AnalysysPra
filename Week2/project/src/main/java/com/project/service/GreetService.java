package com.project.service;

import org.springframework.stereotype.Service;

import java.io.Serial;

@Service // 替代 @MyComponent，告诉 Spring 这是一个业务层 Bean
public class GreetService {
    public String getGreeting(){
        return "<h1>Greetings from GreetingService</h1>";
    }
}
