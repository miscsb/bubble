package dev.miscsb.dating;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.time.Duration;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import dev.miscsb.dating.configuration.RedisConfig;
import dev.miscsb.dating.endpoints.BubbleEndpoint;
import dev.miscsb.dating.endpoints.MatchingEndpoint;
import dev.miscsb.dating.endpoints.ProfileEndpoint;
import dev.miscsb.dating.model.Bubble;
import dev.miscsb.dating.model.Profile;
import static dev.miscsb.dating.FunctionalUtils.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootTest(classes = { DatingApplication.class, RedisConfig.class })
class DatingApplicationTests {

	@Autowired
	ProfileEndpoint profileEndpoint;
	@Autowired
	BubbleEndpoint bubbleEndpoint;
	@Autowired
	MatchingEndpoint matchingEndpoint;

	// @Test
	// public void testProfileCreation() {
	// String profileId = profileEndpoint.createUser("Samuel", "B.", "he/him",
	// "Male", List.of("Female"), 2005, "fun loving dude in the nyc area").block();
	// Profile profile = profileEndpoint.getUser(profileId).block();
	// assertTrue(profile.firstName().equals("Samuel"));

	// String bubbleId = bubbleEndpoint.createBubble("New York", 0, 0).block();
	// Bubble bubble = bubbleEndpoint.getBubble(bubbleId).block();
	// assertTrue(bubble.bubbleName().equals("New York"));

	// bubbleEndpoint.attachUserToBubble(profileId, bubbleId).block();
	// }

	@Test
	public void testMatching() {
		var mb1 = bubbleEndpoint
			.createBubble("New York", 0.0, 0.0);
		var mb2 = bubbleEndpoint
			.createBubble("San Francisco", 10.0, 0.0);
		var m1 = profileEndpoint
			.createUser("John", "D.", "he/him", "Male", List.of("Female"), 2000, "Fun loving dude in the NYC area");
		var m2 = profileEndpoint
			.createUser("Jane", "D.", "she/her", "Female", List.of("Male"), 1999, "Fun loving girl in the NYC area");
		var m3 = profileEndpoint
			.createUser("Jake", "P.", "he/him", "Male", List.of("Female, Male"), 1999, "Fun loving girl in the SF area");
		var res = Mono.zip(mb1, mb2).flux().flatMap(bubbles -> Mono.zip(m1, m2, m3).flux().flatMap(users -> {
				var a1 = bubbleEndpoint.attachUserToBubble(users.getT1(), bubbles.getT1());
				var a2 = bubbleEndpoint.attachUserToBubble(users.getT2(), bubbles.getT1());
				var a3 = bubbleEndpoint.attachUserToBubble(users.getT3(), bubbles.getT1());
				var match = matchingEndpoint.getMatches(users.getT3());
				return Mono.zip(a1, a2, a3).flux().flatMap(constant(match));
			})
		).flatMap(profileEndpoint::getUser);
		System.out.println(res.collectList().block(Duration.ofSeconds(10)));
	}

	@Test
	void contextLoads() {
	}

}
