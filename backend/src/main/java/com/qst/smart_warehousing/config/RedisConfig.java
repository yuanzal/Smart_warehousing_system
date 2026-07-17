package com.qst.smart_warehousing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // 配置连接工厂
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 1. 配置 Key 的序列化器：String 格式（确保 key 在 redis-cli 中可读）
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringSerializer);
        redisTemplate.setHashKeySerializer(stringSerializer);

        // 2. 配置 Value 的序列化器：String 格式（存储 JSON 字符串，保持可读性）
        //    这样存储和读取的都是纯字符串，避免 Jackson 反序列化问题
        redisTemplate.setValueSerializer(stringSerializer);
        redisTemplate.setHashValueSerializer(stringSerializer);

        // 初始化 RedisTemplate
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}