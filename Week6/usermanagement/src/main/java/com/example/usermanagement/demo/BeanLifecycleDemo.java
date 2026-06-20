package com.example.usermanagement.demo;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
// import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component // 让Spring扫描并注册为Bean
// @Profile("bean-lifecycle-demo")
public class BeanLifecycleDemo implements InitializingBean, DisposableBean {

    public BeanLifecycleDemo() {
        System.out.println("1. [构造器] BeanLifecycleDemo 实例化");
    }

	@PostConstruct
	public void postConstruct() {
		System.out.println("2. [@PostConstruct] 初始化方法");
	}

	@Override
	public void afterPropertiesSet() {
		System.out.println("3. [InitializingBean.afterPropertiesSet()] 初始化");

	}

    // 业务方法，模拟Bean的正常使用
    public void doSomething() {
        System.out.println("4. [业务方法] doSomething 被调用，Bean 已完全初始化");
    }

	@PreDestroy
	public void preDestroy() {
		System.out.println("5. [@PreDestroy] 销毁前回调");
	}

	@Override
	public void destroy() {
		System.out.println("6. [DisposableBean.destroy()] 销毁回调");

	}

}
