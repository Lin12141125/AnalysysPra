package com.project.controller;

import com.project.RequestMapping;
import com.project.service.GreetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller // 替代 @MyComponent
public class HelloController {

    @Autowired // 替代 @MyComponent
    private GreetService greetService;
    
    @RequestMapping("/hello")
    public String hello() {
        return "<h1>Hello Controller</h1>";
    }

    @RequestMapping("/greet")
    public String greet() {
        return greetService.getGreeting();
    }
}
