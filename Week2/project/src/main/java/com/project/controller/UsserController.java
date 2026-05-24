package com.project.controller;

import com.project.RequestMapping;
import org.springframework.stereotype.Controller;

@Controller
public class UsserController {

    @RequestMapping("/user")
    public String user() {
        return "<h1>User Controller</h1>";
    }
}
