package com.example.usermanagement.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class LifeCycleDemoRunner implements CommandLineRunner {
    private final BeanLifecycleDemo beanLifecycleDemo;

    public LifeCycleDemoRunner(BeanLifecycleDemo beanLifecycleDemo) {
        this.beanLifecycleDemo = beanLifecycleDemo;
    }

    public void run(String... args) throws Exception {
        beanLifecycleDemo.doSomething();
    }
}
