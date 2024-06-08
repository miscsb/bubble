package dev.miscsb.dating;

import java.time.Duration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import io.lettuce.core.ClientOptions;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class DatingApplication {

	private static final Log LOG = LogFactory.getLog(DatingApplication.class);

	@Bean
	public LettuceConnectionFactory redisConnectionFactory() {
		LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
			.useSsl().and()
			.commandTimeout(Duration.ofSeconds(2))
			.shutdownTimeout(Duration.ZERO)
			.build();

		return new LettuceConnectionFactory(new RedisStandaloneConfiguration("redis", 6379), clientConfig);
	}

	public static void main(String[] args) {
		SpringApplication.run(DatingApplication.class, args);

		LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory();
		connectionFactory.afterPropertiesSet();

		ReactiveRedisTemplate<String, String> template = new ReactiveRedisTemplate<>(connectionFactory,
				RedisSerializationContext.string());

		Mono<Boolean> set = template.opsForValue().set("foo", "bar");
		set.block(Duration.ofSeconds(10));

		LOG.info("Value at foo: " + template.opsForValue().get("foo").block(Duration.ofSeconds(10)));
		connectionFactory.destroy();

	}

}
