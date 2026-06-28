package com.example.usermanagement.cache;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.usermanagement.vo.ProjectDetailVO;
import com.example.usermanagement.vo.ProjectListVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

// 复用UserCacheManager的缓存逻辑，增加了对项目详情和项目列表的缓存支持
@Component
public class ProjectCacheManager {

    private static final String PROJECT_DETAIL_CACHE_KEY_PREFIX = "cache:project:detail:";
    private static final String PROJECT_LIST_CACHE_KEY_PREFIX = "cache:project:list:";
    private static final String PROJECT_DETAIL_LOCK_KEY_PREFIX = "lock:project:detail:";
    private static final String PROJECT_LIST_LOCK_KEY_PREFIX = "lock:project:list:";
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

    @Value("${cache.project.base-ttl-seconds:1800}")
    private long baseTtlSeconds;

    @Value("${cache.project.null-ttl-seconds:120}")
    private long nullTtlSeconds;

    @Value("${cache.project.random-ttl-seconds:300}")
    private long randomTtlSeconds;

    @Value("${cache.project.lock-ttl-seconds:10}")
    private long lockTtlSeconds;

    @Value("${cache.project.lock-retry-times:5}")
    private int lockRetryTimes;

    @Value("${cache.project.lock-retry-sleep-millis:50}")
    private long lockRetrySleepMillis;

    public ProjectDetailVO queryProjectDetail(Integer projectId, Integer userId, Supplier<ProjectDetailVO> dbLoader) {
        String cacheKey = buildProjectDetailKey(projectId, userId);
        String lockKey = PROJECT_DETAIL_LOCK_KEY_PREFIX + projectId + ":user:" + userId;
        return queryWithCache(
                cacheKey,
                lockKey,
                dbLoader,
                this::deserializeProjectDetail,
                this::cacheProjectDetail
        );
    }

    public Page<ProjectListVO> queryMyProjects(Integer userId, Integer page, Integer size, Supplier<Page<ProjectListVO>> dbLoader) {
        String cacheKey = buildMyProjectsKey(userId, page, size);
        String lockKey = PROJECT_LIST_LOCK_KEY_PREFIX + "user:" + userId + ":page:" + page + ":size:" + size;
        return queryWithCache(
                cacheKey,
                lockKey,
                dbLoader,
                this::deserializeProjectListPage,
                this::cacheProjectListPage
        );
    }

    public void evictProject(Integer projectId) {
        deleteByPattern(PROJECT_DETAIL_CACHE_KEY_PREFIX + projectId + ":user:*");
    }

    public void evictMyProjects(Integer userId) {
        deleteByPattern(PROJECT_LIST_CACHE_KEY_PREFIX + "user:" + userId + ":*");
    }

    private String buildProjectDetailKey(Integer projectId, Integer userId) {
        return PROJECT_DETAIL_CACHE_KEY_PREFIX + projectId + ":user:" + userId;
    }

    private String buildMyProjectsKey(Integer userId, Integer page, Integer size) {
        return PROJECT_LIST_CACHE_KEY_PREFIX + "user:" + userId + ":page:" + page + ":size:" + size;
    }

    private <T> T queryWithCache(
            String cacheKey,
            String lockKey,
            Supplier<T> dbLoader,
            CacheDeserializer<T> deserializer,
            CacheWriter<T> cacheWriter) {
        String cachedRaw = stringRedisTemplate.opsForValue().get(cacheKey);
        T cachedValue = deserializer.deserialize(cachedRaw);
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
                T retryValue = deserializer.deserialize(retryRaw);
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
            T doubleCheckValue = deserializer.deserialize(doubleCheckRaw);
            if (doubleCheckValue != null) {
                return doubleCheckValue;
            }
            if (NULL_MARKER.equals(doubleCheckRaw)) {
                return null;
            }

            T dbValue = dbLoader.get();
            if (dbValue == null) {
                cacheNull(cacheKey);
                return null;
            }

            cacheWriter.write(cacheKey, dbValue);
            return dbValue;
        } finally {
            if (locked) {
                unlock(lockKey, lockValue);
            }
        }
    }

    private ProjectDetailVO deserializeProjectDetail(String raw) {
        if (!StringUtils.hasText(raw) || NULL_MARKER.equals(raw)) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, ProjectDetailVO.class);
        } catch (Exception e) {
            return null;
        }
    }

    private Page<ProjectListVO> deserializeProjectListPage(String raw) {
        if (!StringUtils.hasText(raw) || NULL_MARKER.equals(raw)) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<Page<ProjectListVO>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private void cacheProjectDetail(String cacheKey, ProjectDetailVO projectDetail) {
        cacheObject(cacheKey, projectDetail);
    }

    private void cacheProjectListPage(String cacheKey, Page<ProjectListVO> projectPage) {
        cacheObject(cacheKey, projectPage);
    }

    private void cacheObject(String cacheKey, Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            long ttl = baseTtlSeconds + ThreadLocalRandom.current().nextLong(randomTtlSeconds + 1);
            stringRedisTemplate.opsForValue().set(cacheKey, json, Duration.ofSeconds(ttl));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("项目缓存序列化失败", e);
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

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @FunctionalInterface
    private interface CacheDeserializer<T> {
        T deserialize(String raw);
    }

    @FunctionalInterface
    private interface CacheWriter<T> {
        void write(String cacheKey, T value);
    }
}
