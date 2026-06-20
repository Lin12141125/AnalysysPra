package com.example.usermanagement.demo;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("bean-lifecycle-demo")
public class BeanLifecycleDemo implements InitializingBean, DisposableBean {

	@PostConstruct
	public void postConstruct() {
		System.out.println("[BeanLifecycleDemo] @PostConstruct");
	}

	@Override
	public void afterPropertiesSet() {
		System.out.println("[BeanLifecycleDemo] InitializingBean.afterPropertiesSet()");
	}

	@PreDestroy
	public void preDestroy() {
		System.out.println("[BeanLifecycleDemo] @PreDestroy");
	}

	@Override
	public void destroy() {
		System.out.println("[BeanLifecycleDemo] DisposableBean.destroy()");
	}

}
