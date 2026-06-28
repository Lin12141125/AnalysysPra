package com.example.usermanagement.cache;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.usermanagement.vo.TaskVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class TaskCacheManager {
    private static final String TASK_LIST_CACHE_KEY_PREFIX = "cache:task:list:";
    private static final String TASK_LIST_LOCK_KEY_PREFIX = "lock:task:list:";
    private static final String NULL_MARKER = "__NULL__";

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "   return redis.call('del', KEYS[1]) " +
                "else " +
                "   return 0 " +
                "end"
        );
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${cache.task.base-ttl-seconds:1800}")
    private long baseTtlSeconds;

    @Value("${cache.task.null-ttl-seconds:120}")
    private long nullTtlSeconds;

    @Value("${cache.task.random-ttl-seconds:300}")
    private long randomTtlSeconds;

    @Value("${cache.task.lock-ttl-seconds:10}")
    private long lockTtlSeconds;

    @Value("${cache.task.lock-retry-times:5}")
    private int lockRetryTimes;

    @Value("${cache.task.lock-retry-sleep-millis:50}")
    private long lockRetrySleepMillis;

    public Page<TaskVO> queryTaskPage(
            Integer projectId,
            Integer page,
            Integer size,
            String status,
            String priority,
            Integer assigneeId,
            Supplier<Page<TaskVO>> dbLoader) {
        String cacheKey = buildTaskListKey(projectId, page, size, status, priority, assigneeId);
        String lockKey = TASK_LIST_LOCK_KEY_PREFIX + "project:" + projectId + ":page:" + page + ":size:" + size
                + ":status:" + normalize(status)
                + ":priority:" + normalize(priority)
                + ":assignee:" + normalize(assigneeId);

        return queryWithCache(cacheKey, lockKey, dbLoader);
    }

    public void evictTaskList(Integer projectId) {
        deleteByPattern(TASK_LIST_CACHE_KEY_PREFIX + "project:" + projectId + ":*");
    }

    private String buildTaskListKey(Integer projectId, Integer page, Integer size, String status, String priority, Integer assigneeId) {
        return TASK_LIST_CACHE_KEY_PREFIX + "project:" + projectId
                + ":page:" + page
                + ":size:" + size
                + ":status:" + normalize(status)
                + ":priority:" + normalize(priority)
                + ":assignee:" + normalize(assigneeId);
    }

    private Page<TaskVO> queryWithCache(String cacheKey, String lockKey, Supplier<Page<TaskVO>> dbLoader) {
        String cachedRaw = stringRedisTemplate.opsForValue().get(cacheKey);
        Page<TaskVO> cachedValue = deserializeTaskPage(cachedRaw);
        if (cachedValue != null) {
            return cachedValue;
        }
        if (NULL_MARKER.equals(cachedRaw)) {
            return null;
        }

        String lockValue = UUID.randomUUID().toString();
        boolean locked = false;

        try {
            for (int i = 0; i < lockRetryTimes; i++) {
                locked = tryLock(lockKey, lockValue);
                if (locked) {
                    break;
                }

                sleepQuietly(lockRetrySleepMillis);

                String retryRaw = stringRedisTemplate.opsForValue().get(cacheKey);
                Page<TaskVO> retryValue = deserializeTaskPage(retryRaw);
                if (retryValue != null) {
                    return retryValue;
                }
                if (NULL_MARKER.equals(retryRaw)) {
                    return null;
                }
            }

            if (!locked) {
                return dbLoader.get();
            }

            String doubleCheckRaw = stringRedisTemplate.opsForValue().get(cacheKey);
            Page<TaskVO> doubleCheckValue = deserializeTaskPage(doubleCheckRaw);
            if (doubleCheckValue != null) {
                return doubleCheckValue;
            }
            if (NULL_MARKER.equals(doubleCheckRaw)) {
                return null;
            }

            Page<TaskVO> dbValue = dbLoader.get();
            if (dbValue == null) {
                cacheNull(cacheKey);
                return null;
            }

            cacheTaskPage(cacheKey, dbValue);
            return dbValue;
        } finally {
            if (locked) {
                unlock(lockKey, lockValue);
            }
        }
    }

    private Page<TaskVO> deserializeTaskPage(String raw) {
        if (!StringUtils.hasText(raw) || NULL_MARKER.equals(raw)) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<Page<TaskVO>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private void cacheTaskPage(String cacheKey, Page<TaskVO> taskPage) {
        try {
            String json = objectMapper.writeValueAsString(taskPage);
            long ttl = baseTtlSeconds + ThreadLocalRandom.current().nextLong(randomTtlSeconds + 1);
            stringRedisTemplate.opsForValue().set(cacheKey, json, Duration.ofSeconds(ttl));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("任务列表缓存序列化失败", e);
        }
    }

    private void cacheNull(String cacheKey) {
        long extra = Math.min(30, Math.max(1, randomTtlSeconds));
        long ttl = nullTtlSeconds + ThreadLocalRandom.current().nextLong(extra + 1);
        stringRedisTemplate.opsForValue().set(cacheKey, NULL_MARKER, Duration.ofSeconds(ttl));
    }

    private boolean tryLock(String lockKey, String lockValue) {
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(
                lockKey,
                lockValue,
                Duration.ofSeconds(lockTtlSeconds)
        );
        return Boolean.TRUE.equals(ok);
    }

    private void unlock(String lockKey, String lockValue) {
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(lockKey),
                lockValue
        );
    }

    private void deleteByPattern(String pattern) {
        Set<String> keys = stringRedisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    private String normalize(Object value) {
        return value == null ? "all" : value.toString();
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
