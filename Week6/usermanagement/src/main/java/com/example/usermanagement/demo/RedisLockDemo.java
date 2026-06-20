package com.example.usermanagement.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;


@Component
@Profile("redis-lock-demo") 
public class RedisLockDemo implements CommandLineRunner {

    private static final String LOCK_KEY = "redis_lock_demo:task7";
    private static final long LOCK_EXPIRE_SECONDS = 10;

    @Autowired
    private StringRedisTemplate redisTemplate; // 用于操作Redis

    @Override
    public void run(String... args) throws Exception {
        System.out.println("\n[RedisLockDemo]==============================");
        System.out.println("[RedisLockDemo] Redis 命令语义：SET " + LOCK_KEY + " value NX EX " + LOCK_EXPIRE_SECONDS);

        // 使用CountDownLatch等待两个线程执行完毕
        CountDownLatch latch = new CountDownLatch(2);

        // 启动两个线程模拟并发争抢锁
        for (int i = 1; i <= 2; i++){
            final int threadId = i;
            new Thread(() -> {
                try{
                    // 每个线程生成唯一标识，作为锁的value
                    String value=UUID.randomUUID().toString();
                    // 尝试获取锁：SET key value NX EX seconds
                    Boolean success = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, value, Duration.ofSeconds(LOCK_EXPIRE_SECONDS));
                    if (Boolean.TRUE.equals(success)){
                        System.out.println("[RedisLockDemo] 线程 " + threadId + " 获取锁成功，value=" + value);
                        // 模拟持锁业务执行时间
                        try{
                            Thread.sleep(2000); // 假设业务耗时(持锁)2秒
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        System.out.println("[RedisLockDemo][线程" + threadId + "] 业务处理完成，准备释放锁...");
                        // 释放锁（校验value避免误删）
                        String currentValue = redisTemplate.opsForValue().get(LOCK_KEY);
                        if (value.equals(currentValue)){
                            redisTemplate.delete(LOCK_KEY);
                            System.out.println("[RedisLockDemo] 线程 " + threadId + " 释放锁成功");
                        } else {
                            System.out.println("[RedisLockDemo] 线程 " + threadId + " 释放锁失败，锁已被其他线程持有或过期");
                        }
                    } else {
                        System.out.println("[RedisLockDemo] 线程 " + threadId + " 获取锁失败，说明锁已被其他线程持有");
                    }
                } finally {
                    latch.countDown(); // 线程结束，计数-1
                }
            }).start();
        }
        latch.await(); // 等待两个线程执行完毕
        System.out.println("[RedisLockDemo] ===== 结束 =====");
        System.exit(0);
    }
}
