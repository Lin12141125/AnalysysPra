package com.example.usermanagement.demo.beanLifecycle;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("bean-lifecycle-demo")
public class LifeCycleDemoRunner implements CommandLineRunner {
    private final BeanLifecycleDemo beanLifecycleDemo;

    public LifeCycleDemoRunner(BeanLifecycleDemo beanLifecycleDemo) {
        this.beanLifecycleDemo = beanLifecycleDemo;
    }

    public void run(String... args) throws Exception {
        beanLifecycleDemo.doSomething();
    }
}
