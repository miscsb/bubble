package com.miscsb.bubble;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@TestConfiguration(proxyBeanMethods = false)
@Testcontainers
public class TestConfig {

	private static final Logger logger = LoggerFactory.getLogger(TestConfig.class);

	private static final GenericContainer<?> container;
	public static final int REDIS_PORT = 6379;
	public static final String REDIS_URI = "redis://localhost:" + REDIS_PORT;
	static {
		container = new GenericContainer<>("redis/redis-stack:latest").withExposedPorts(TestConfig.REDIS_PORT);
		container.start();
		System.setProperty("spring.data.redis.host", container.getHost());
		System.setProperty("spring.data.redis.port", container.getMappedPort(REDIS_PORT) + "");
	}

}
