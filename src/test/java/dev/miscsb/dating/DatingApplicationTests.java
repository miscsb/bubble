package dev.miscsb.dating;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import dev.miscsb.dating.configuration.RedisConfig;

@SpringBootTest(classes={ DatingApplication.class, RedisConfig.class })
class DatingApplicationTests {

	@Test
	void contextLoads() {
	}

}
