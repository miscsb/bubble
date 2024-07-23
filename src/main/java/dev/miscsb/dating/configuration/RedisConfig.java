package dev.miscsb.dating.configuration;

import io.lettuce.core.ClientOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import dev.miscsb.dating.model.Bubble;
import dev.miscsb.dating.model.Profile;

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

    /**
     * Redis configuration
     *
     * @return redisStandaloneConfiguration
     */
    @Bean
    public RedisStandaloneConfiguration redisStandaloneConfiguration() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(url, port);
        return redisStandaloneConfiguration;
    }

    /**
     * Client Options
     * Reject requests when redis is in disconnected state and
     * Redis will retry to connect automatically when redis server is down
     *
     * @return client options
     */
    @Bean
    public ClientOptions clientOptions() {
        return ClientOptions.builder()
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .autoReconnect(true)
                .build();
    }

    @Bean
    public ReactiveRedisTemplate<String, Profile> profiles(ReactiveRedisConnectionFactory connectionFactory) {
        Jackson2JsonRedisSerializer<Profile> serializer = new Jackson2JsonRedisSerializer<>(Profile.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, Profile> builder =
            RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

        RedisSerializationContext<String, Profile> context = builder.value(serializer).build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, Bubble> bubbles(ReactiveRedisConnectionFactory connectionFactory) {
        Jackson2JsonRedisSerializer<Bubble> serializer = new Jackson2JsonRedisSerializer<>(Bubble.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, Bubble> builder =
            RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

        RedisSerializationContext<String, Bubble> context = builder.value(serializer).build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }

}
