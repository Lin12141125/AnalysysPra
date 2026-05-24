package com.task5_7.service;

import org.springframework.stereotype.Component;

@Component
public class MessageService {
    public String getMessage() {
        System.out.println("MessageService.getMessage运行中");
        return "Hello, World!";
    }
}
