package com.project.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {
    // 拦截所有带有@RequestMapping注解的方法（控制器方法）

    // @Around("@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    // @Around("execution(* com.project.service.*.*(..))")
    @Around("@annotation(com.project.RequestMapping)")
    public Object log(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();
        System.out.println("[\uD83D\uDD35 AOP] 方法开始：" + methodName);
        // 执行目标方法
        Object result = joinPoint.proceed();
        long endTime = System.currentTimeMillis();
        System.out.println("[\uD83D\uDD34 AOP] 方法结束：" + methodName+", 耗时："+(endTime - startTime)+"ms");

        return result;
    }
}
