package com.miscsb.bubble;

import static org.junit.Assert.*;

import java.util.List;
import java.time.Duration;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.miscsb.bubble.configuration.RedisConfig;
import com.miscsb.bubble.controller.BubbleController;
import com.miscsb.bubble.controller.MatchingController;
import com.miscsb.bubble.controller.ProfileController;
import com.miscsb.bubble.model.Bubble;
import com.miscsb.bubble.model.Profile;

import static com.miscsb.bubble.FunctionalUtils.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootTest(classes = { DatingApplication.class, RedisConfig.class })
class DatingApplicationTests {

	@Autowired
	ProfileController profileEndpoint;
	@Autowired
	BubbleController bubbleEndpoint;
	@Autowired
	MatchingController matchingEndpoint;

	// @Test
	// public void testProfilesSynchronous() {
	// Profile profileSent = new Profile("Samuel", "B.", "he/him", "Male",
	// List.of("Female"), 2005, "fun loving dude in the nyc area");
	// String profileId = profileEndpoint.createUser(profileSent).block();
	// Profile profileReceived = profileEndpoint.getUser(profileId).block();
	// assertTrue(profileReceived.equals(profileSent));

	// Bubble bubbleSent = new Bubble("New York", 0, 0);
	// String bubbleId = bubbleEndpoint.createBubble(bubbleSent).block();
	// Bubble bubbleReceived = bubbleEndpoint.getBubble(bubbleId).block();
	// assertTrue(bubbleReceived.equals(bubbleSent));

	// bubbleEndpoint.attachUserToBubble(profileId, bubbleId).block();
	// }

	@Test
	public void testMatchingSynchronous() {
		String bid1 = bubbleEndpoint.createBubble(new Bubble("NY", 0.0, 0.0)).block();
		String bid2 = bubbleEndpoint.createBubble(new Bubble("SF", 10.0, 0.0)).block();
		String uid1 = profileEndpoint.createUser(
				new Profile("John", "D.", "he/him", "Male", List.of("Female"), 2000, "Fun loving dude in the NYC area"))
				.block();
		String uid2 = profileEndpoint.createUser(new Profile("Jane", "D.", "she/her", "Female", List.of("Male"), 1999,
				"Fun loving girl in the NYC area")).block();
		String uid3 = profileEndpoint.createUser(new Profile("Jake", "P.", "he/him", "Male", List.of("Female", "Male"),
				1999, "Fun loving girl in the SF area")).block();

		bubbleEndpoint.attachUserToBubble(uid1, bid1).block();
		bubbleEndpoint.attachUserToBubble(uid2, bid1).block();
		bubbleEndpoint.attachUserToBubble(uid3, bid1).block();
		var list1 = matchingEndpoint.getMatches(uid3).collectList().block(Duration.ofSeconds(5));

		bubbleEndpoint.attachUserToBubble(uid3, bid2).block();
		var list2 = matchingEndpoint.getMatches(uid3).collectList().block(Duration.ofSeconds(5));

		assertTrue(list1.equals(List.of("2")));
		assertTrue(list2.equals(List.of()));
	}

	@Test
	public void testBubbleUpdateSynchronous() {
		Bubble ver1 = new Bubble("PARTY CITY!", 0.0, 0.0);
		String bid = bubbleEndpoint.createBubble(ver1).block();
		Bubble returned1 = bubbleEndpoint.getBubble(bid).block();
		assertEquals(ver1, returned1);
		
		Bubble ver2 = new Bubble("doomer city", 0.0, 0.0);
		bubbleEndpoint.updateBubble(bid, ver2).block();
		Bubble returned2 = bubbleEndpoint.getBubble(bid).block();
		assertEquals(ver2, returned2);
	}

	@Test
	void contextLoads() {
	}

}
