package dev.miscsb.dating;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import dev.miscsb.dating.configuration.RedisConfig;
import dev.miscsb.dating.endpoints.ProfileEndpoint;
import dev.miscsb.dating.model.Profile;

@SpringBootTest(classes={ DatingApplication.class, RedisConfig.class })
class DatingApplicationTests {

	@Autowired ProfileEndpoint endpoint;

    @Test
    public void testProfileCreation() {
		String createdId = endpoint.createUser("Samuel", "B.", "he/him", "Male", List.of("Female"), 2005, "fun loving dude in the nyc area").block();
		Profile createdProfile = endpoint.getUser(createdId).block();
		assertTrue(createdProfile.firstName().equals("Samuel"));
    }

	@Test
	void contextLoads() {
	}

}
