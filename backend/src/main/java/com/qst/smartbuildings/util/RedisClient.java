package com.qst.smartbuildings.util;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

// 标记为组件，让 Spring 扫描并管理
@Component
public class RedisClient {

    // 注入 Spring 自动配置的 RedisTemplate
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 封装常用方法：设置键值对
    public void set(String key, Object value) {

        redisTemplate.opsForValue().set(key, value);

    }

    // 设置键值对并指定过期时间
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    // 获取值
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }
    // 删除键
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    // 在 RedisClient 中新增：按前缀筛选所有 key（用 SCAN 避免阻塞）
    public Set<String> getKeysByPrefix(String prefix) {
        Set<String> matchedKeys = new HashSet<>();
        // SCAN 命令：每次扫描 100 个 key，避免一次性扫描大量 key 导致 Redis 阻塞
        redisTemplate.execute((RedisConnection connection) -> {
            try (Cursor<byte[]> cursor = connection.scan(
                    ScanOptions.scanOptions()
                            .match(prefix + "*") // 前缀匹配规则：前缀 + 通配符 *
                            .count(100)         // 每次扫描的数量
                            .build()
            )) {
                // 遍历游标，转换 byte[] 为 String 类型的 key
                cursor.forEachRemaining(bytes -> {
                    String key = new String(bytes, StandardCharsets.UTF_8);
                    matchedKeys.add(key);
                });
                return null;
            } catch (Exception e) {
                throw new RuntimeException("按前缀筛选 key 失败", e);
            }
        });
        return matchedKeys;
    }

}