package com.task5_7.aspect;


import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect // 切面类
@Component // 让Spring管理此切面
public class LoggingAspect {

    @Around("execution(* com.task5_7.service.*.*(..))") // 环绕通知：拦截 com.task5_7.service 包下所有类的所有方法
    public Object logMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        // 获取方法名
        String methodName = joinPoint.getSignature().toShortString();

        System.out.println("[\uD83D\uDD35 AOP] 方法开始：" + methodName);
        // 执行目标方法
        Object result = joinPoint.proceed();
        long endTime = System.currentTimeMillis();
        System.out.println("[\uD83D\uDD34 AOP] 方法结束：" + methodName+", 耗时："+(endTime - startTime)+"ms");
        // 返回结果
        return result;
    }
}
