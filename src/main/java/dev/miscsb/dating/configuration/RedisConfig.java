package dev.miscsb.dating.configuration;

import io.lettuce.core.ClientOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import dev.miscsb.dating.model.Profile;

@Configuration
@Primary
@EnableRedisRepositories("dev.miscsb.dating.repository")
public class RedisConfig {

    private final String url;
    private final int port;
    private final String username;
    private final String password;

    public RedisConfig(@Value("${spring.data.redis.host}") String url,
                       @Value("${spring.data.redis.port}") int port,
                       @Value("${spring.data.redis.username}") String username,
                       @Value("${spring.data.redis.password}") String password) {
        this.url = url;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    /**
     * Redis configuration
     *
     * @return redisStandaloneConfiguration
     */
    @Bean
    public RedisStandaloneConfiguration redisStandaloneConfiguration() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(url, port);
        redisStandaloneConfiguration.setUsername(username);
        redisStandaloneConfiguration.setPassword(password);
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
    public ReactiveRedisTemplate<String, Profile> redisOperations(ReactiveRedisConnectionFactory connectionFactory) {
        Jackson2JsonRedisSerializer<Profile> serializer = new Jackson2JsonRedisSerializer<>(Profile.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, Profile> builder =
            RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

        RedisSerializationContext<String, Profile> context = builder.value(serializer).build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }

}
