package com.miscsb.bubble.configuration;

import io.lettuce.core.ClientOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.data.redis.support.collections.DefaultRedisList;
import org.springframework.data.redis.support.collections.RedisList;

import com.miscsb.bubble.util.KeyUtils;
import com.miscsb.bubble.model.Bubble;
import com.miscsb.bubble.model.Profile;

@Configuration
@Primary
public class RedisConfig {

    private final String url;
    private final int port;

    public RedisConfig(@Value("${spring.data.redis.host}") String url,
                       @Value("${spring.data.redis.port}") int port) {
        this.url = url;
        this.port = port;
    }

    @Bean
    RedisStandaloneConfiguration redisStandaloneConfiguration() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(url, port);
        return redisStandaloneConfiguration;
    }

    @Bean
    ClientOptions clientOptions() {
        return ClientOptions.builder()
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .autoReconnect(true)
                .build();
    }

    @Bean
    RedisTemplate<String, Profile> profileTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Profile> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Profile.class));
        return template;
    }

    @Bean
    RedisTemplate<String, Bubble> bubbleTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Bubble> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Bubble.class));
        return template;
    }

    @Bean
    RedisList<String> bubbleIdList(StringRedisTemplate template) {
        return new DefaultRedisList<>(KeyUtils.global("bubbles"), template);
    }

    @Bean
    RedisList<String> userIdList(StringRedisTemplate template) {
        return new DefaultRedisList<>(KeyUtils.global("users"), template);
    }

    @Bean
    RedisAtomicLong bubbleIdCounter(RedisConnectionFactory connectionFactory) {
        return new RedisAtomicLong(KeyUtils.global("bid"), connectionFactory);
    }

    @Bean
    RedisAtomicLong userIdCounter(RedisConnectionFactory connectionFactory) {
        return new RedisAtomicLong(KeyUtils.global("uid"), connectionFactory);
    }
}
