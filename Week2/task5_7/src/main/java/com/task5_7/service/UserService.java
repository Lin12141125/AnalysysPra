package com.task5_7.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserService {
    private final MessageService messageService;

    @Autowired // 构造器注入
    public UserService(MessageService messageService) {
        this.messageService = messageService;
    }

    public UserService() {
        this.messageService = null;
    }

    public void showMessage() {
        System.out.println(messageService.getMessage());
    }
}
