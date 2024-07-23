package dev.miscsb.dating;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import dev.miscsb.dating.configuration.RedisConfig;
import dev.miscsb.dating.endpoints.BubbleEndpoint;
import dev.miscsb.dating.endpoints.ProfileEndpoint;
import dev.miscsb.dating.model.Bubble;
import dev.miscsb.dating.model.Profile;

@SpringBootTest(classes={ DatingApplication.class, RedisConfig.class })
class DatingApplicationTests {

	@Autowired ProfileEndpoint profileEndpoint;
	@Autowired BubbleEndpoint bubbleEndpoint;

    @Test
    public void testProfileCreation() {
		String profileId = profileEndpoint.createUser("Samuel", "B.", "he/him", "Male", List.of("Female"), 2005, "fun loving dude in the nyc area").block();
		Profile profile = profileEndpoint.getUser(profileId).block();
		assertTrue(profile.firstName().equals("Samuel"));

		String bubbleId = bubbleEndpoint.createBubble("New York", 0, 0).block();
		Bubble bubble = bubbleEndpoint.getBubble(bubbleId).block();
		assertTrue(bubble.bubbleName().equals("New York"));

		bubbleEndpoint.attachUserToBubble(profileId, bubbleId).block();
    }

	@Test
	void contextLoads() {
	}

}
