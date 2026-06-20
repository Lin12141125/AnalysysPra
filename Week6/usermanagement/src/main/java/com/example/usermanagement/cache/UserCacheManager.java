package com.example.usermanagement.cache;

import com.example.usermanagement.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * 缓存核心类
 */
@Component
public class UserCacheManager {
    // 常量定义
    private static final String USER_CACHE_KEY_PREFIX = "cache:user:"; // 用户数据缓存key前缀，最终key为cache:user:{id}
    private static final String USER_LOCK_KEY_PREFIX = "lock:user:"; // 分布式锁key前缀，最终key为lock:user:{id}
    private static final String NULL_MARKER = "__NULL__"; // 控制标记，当数据库不存在该用户时，缓存此字符串表示空值，避免缓存穿透

    /* 
     * Lua脚本：原子性地检查锁的值并删除锁
     * 使用Lua脚本将“查询value-->比较valua-->删除key”三步合并为原子操作
     * 只有锁的持有者（value匹配）才能释放锁，避免误删他人锁导致的缓存击穿问题
     */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        // KEYS[1]-->锁的key，ARGV[1]-->期望的的value（即线程持有的锁标识）
        UNLOCK_SCRIPT.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "   return redis.call('del', KEYS[1]) " +
                "else " +
                "   return 0 " +
                "end"
        );
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    // 依赖注入
    @Autowired
    private StringRedisTemplate stringRedisTemplate; // 操作Redis，默认使用String序列化

    @Autowired
    private ObjectMapper objectMapper; // 用于User对象与JSON字符串之间的转换

    // 缓存配置参数注入（可通过application.properties进行调整）
    @Value("${cache.user.base-ttl-seconds:1800}")
    private long baseTtlSeconds;

    @Value("${cache.user.null-ttl-seconds:120}")
    private long nullTtlSeconds;

    @Value("${cache.user.random-ttl-seconds:300}")
    private long randomTtlSeconds;

    @Value("${cache.user.lock-ttl-seconds:10}")
    private long lockTtlSeconds;

    @Value("${cache.user.lock-retry-times:5}")
    private int lockRetryTimes;

    @Value("${cache.user.lock-retry-sleep-millis:50}")
    private long lockRetrySleepMillis;

    /*
     * 三种问题的统一入口，流程：
     * 1. 先读缓存
     * 2. 命中空值标记则直接返回 null（防穿透）
     * 3. 未命中则尝试加锁（防击穿）
     * 4. 加锁失败时短暂重试并再次读缓存
     * 5. 加锁成功后双检缓存，仍未命中才查 DB
     * 6. 查到数据则写缓存并加随机 TTL（防雪崩）
     * 7. 查不到写空值缓存（防穿透）
     * 8. finally 中释放锁
     */

    // id: 用户id, dbLoader: 回调函数，从数据库加载用户数据
    public User queryUserById(Integer id, Supplier<User> dbLoader) {
        // 构建缓存key
        String cacheKey = USER_CACHE_KEY_PREFIX + id;

        // 先读缓存（第一次检查）
        // 从Redis获取原始字符传-->这里的缓存值可能是用户数据的JSON字符串，也可能是NULL_MARKER（表示数据库不存在该用户），也可能是null（表示缓存未命中）
        String cachedRaw = stringRedisTemplate.opsForValue().get(cacheKey);
        // 尝试反序列化为User对象，如果反序列化失败或缓存值为NULL_MARKER，则返回null
        User cachedUser = deserializeUser(cachedRaw);
        if (cachedUser != null) {
            return cachedUser; // 命中真实数据，直接返回
        }
        if (NULL_MARKER.equals(cachedRaw)) {
            return null; // 命中空值标记（数据库不存在该用户），直接返回null
        }

        // 缓存未命中（即没有真实数据，也没有空值标记）-->key不存在或已过期
        // -->需要回源DB加载数据-->防止击穿，使用分布式锁控制并发
        String lockKey = USER_LOCK_KEY_PREFIX + id;
        String lockValue = UUID.randomUUID().toString(); // 每个线程的唯一标识，用于释放锁（防误删）
        boolean locked = false;

        try {
            // 若加锁失败则有限次重试获取锁，并在每次重试前短暂休眠，重试时再次读取缓存（第二次检查），如果此时缓存已被其他线程加载出来了，则直接返回结果，避免不必要的DB压力
            for (int i = 0; i < lockRetryTimes; i++) {
                locked = tryLock(lockKey, lockValue);
                if (locked) {
                    break; // 获取锁成功，退出重试
                }
                sleepQuietly(lockRetrySleepMillis); // 获取锁失败，短暂休眠后重试

                 // 重试期间其他线程可以能已经重建了缓存-->重试时再次读取缓存（第二次检查），如果此时缓存已被其他线程加载出来了，则直接返回结果，避免不必要的DB压力
                String retryRaw = stringRedisTemplate.opsForValue().get(cacheKey);
                User retryUser = deserializeUser(retryRaw);
                if (retryUser != null) {
                    return retryUser;
                }
                if (NULL_MARKER.equals(retryRaw)) {
                    return null;
                }
            }

            // 如果始终未未获取到锁-->直接回源DB加载数据（不再重试锁）-->可能会有短暂的缓存击穿，但避免线程永久阻塞
            if (!locked) {
                return dbLoader.get();
            }

            // 获取锁成功-->再次检查缓存（第三次检查，双检锁）-->如果仍未命中才加载DB数据（防止在获取锁期间其他线程已更新）
            String doubleCheckRaw = stringRedisTemplate.opsForValue().get(cacheKey);
            User doubleCheckUser = deserializeUser(doubleCheckRaw);
            if (doubleCheckUser != null) {
                return doubleCheckUser;
            }
            if (NULL_MARKER.equals(doubleCheckRaw)) {
                return null;
            }

            // 回源DB加载数据（唯一被允许执行查询的线程）
            User dbUser = dbLoader.get();
            if (dbUser == null) {
                // 数据库不存在该用户-->缓存空值标记（防穿透），并返回null，TTL较短
                cacheNull(cacheKey);
                return null;
            }
            // 数据库存在该用户-->缓存用户数据，TTL=基础TTL+随机TTL（防雪崩），并返回用户对象
            cacheUser(cacheKey, dbUser);
            return dbUser;
        } finally {
            // 释放锁（仅当前线程持有锁才释放）
            if (locked) {
                unlock(lockKey, lockValue);
            }
        }
    }

    /*
     * 主动失效缓存[删除指定用户缓存]（更新和删除用户后调用，保证一致性）
     */
    public void evictUser(Integer id) {
        stringRedisTemplate.delete(USER_CACHE_KEY_PREFIX + id);
    }

    /*
     * 获取指定用户缓存的剩余过期时间-->监控/调试
     */
    public Long getUserCacheTtl(Integer id) {
        return stringRedisTemplate.getExpire(USER_CACHE_KEY_PREFIX + id);
    }

    /*
     * 尝试获取分布式锁，成功返回true，失败返回false
     */
    private boolean tryLock(String lockKey, String lockValue) {
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(
                lockKey,
                lockValue,
                Duration.ofSeconds(lockTtlSeconds) // 自动过期，防止死锁
        );
        return Boolean.TRUE.equals(ok);
    }

    /*
     * 安全释放分布式锁（仅当前线程持有锁[value匹配]才释放）
     */
    private void unlock(String lockKey, String lockValue) {
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(lockKey), // KEYS[1]
                lockValue                           // ARGV[1]
        );
    }

    /*
     * 缓存真实用户数据，TTL=基础TTL+随机TTL（防雪崩）
     */
    private void cacheUser(String cacheKey, User user) {
        try {
            String json = objectMapper.writeValueAsString(user);
            // 雪崩防护：基础TTL+随机偏移(0~randomTtlSeconds)
            long ttl = baseTtlSeconds + ThreadLocalRandom.current().nextLong(randomTtlSeconds + 1);
            stringRedisTemplate.opsForValue().set(cacheKey, json, Duration.ofSeconds(ttl));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("用户缓存序列化失败", e); // 序列化失败属于不可恢复的错误，抛出运行时异常
        }
    }

    /*
     * 缓存空值标记（防穿透），TTL=基础TTL+随机TTL(避免大量空值同时过期造成穿透)，但整体TTL较短（nullTtlSeconds）
     */
    private void cacheNull(String cacheKey) {
        // 空值TTL = nullTtlSeconds + 小随机偏移（最多30秒或randomTtlSeconds，取较小值）
        long extra = Math.min(30, Math.max(1, randomTtlSeconds));
        long ttl = nullTtlSeconds + ThreadLocalRandom.current().nextLong(extra + 1);
        stringRedisTemplate.opsForValue().set(cacheKey, NULL_MARKER, Duration.ofSeconds(ttl));
    }

    /*
     * 反序列化Redis中的字符串为User对象
     * @param raw Redis中存储的原始字符串（可能是JSON或NULL_MARKER）
     * @return User对象，若无法反序列化或为null/空标记则返回null
     */
    private User deserializeUser(String raw) {
        // 如果raw为空字符串、纯空白字符串、或空值标记，直接返回null，避免反序列化异常
        if (!StringUtils.hasText(raw) || NULL_MARKER.equals(raw)) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, User.class);
        } catch (Exception e) {
            return null; // 反序列化失败（可能是数据损坏或格式不正确），返回null表示缓存无效，下一步会回源DB加载并重建缓存
        }
    }

    // 线程安全休眠（不抛出InterruptedException）
    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
