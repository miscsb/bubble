package com.miscsb.bubble;

import static org.junit.Assert.*;

import java.util.List;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.miscsb.bubble.configuration.RedisConfig;
import com.miscsb.bubble.controller.BubbleController;
import com.miscsb.bubble.controller.MatchingController;
import com.miscsb.bubble.controller.ProfileController;
import com.miscsb.bubble.model.Bubble;
import com.miscsb.bubble.model.Profile;

@SpringBootTest(classes = { DatingApplication.class, RedisConfig.class })
public class ReactiveControllerTests {

	@Autowired
	ProfileController profileEndpoint;
	@Autowired
	BubbleController bubbleEndpoint;
	@Autowired
	MatchingController matchingEndpoint;

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

		bubbleEndpoint.setUserBubble(uid1, bid1).block();
		bubbleEndpoint.setUserBubble(uid2, bid1).block();
		bubbleEndpoint.setUserBubble(uid3, bid1).block();
		var retreivedBubbleUser3_1 = bubbleEndpoint.getUserBubble(uid3).block();
		var list1 = matchingEndpoint.getMatches(uid3).collectList().block(Duration.ofSeconds(5));
		
		bubbleEndpoint.setUserBubble(uid3, bid2).block();
		var retreivedBubbleUser3_2 = bubbleEndpoint.getUserBubble(uid3).block();
		var list2 = matchingEndpoint.getMatches(uid3).collectList().block(Duration.ofSeconds(5));

		assertEquals(retreivedBubbleUser3_1, bid1);
		assertEquals(retreivedBubbleUser3_2, bid2);
		assertTrue(list1.equals(List.of("2")));
		assertTrue(list2.equals(List.of()));
	}

	@Test
	public void testBubbleCrudSynchronous() {
		Bubble ver1 = new Bubble("PARTY CITY!", 0.0, 0.0);
		String bid = bubbleEndpoint.createBubble(ver1).block();
		Bubble returned1 = bubbleEndpoint.getBubble(bid).block();
		assertEquals(ver1, returned1);
		
		Bubble ver2 = new Bubble("doomer city", 0.0, 0.0);
		bubbleEndpoint.updateBubble(bid, ver2).block();
		Bubble returned2 = bubbleEndpoint.getBubble(bid).block();
		assertEquals(ver2, returned2);

		bubbleEndpoint.deleteBubble(bid).block();
		assertThrows(RuntimeException.class, () -> bubbleEndpoint.getBubble(bid).block());
	}

	@Test
	public void testProfileCrudSynchronous() {
		Profile ver1 = new Profile("Scrooge", "McDuck", "he/him", "Male", List.of("Female"), 1900, "$$$");
		String uid = profileEndpoint.createUser(ver1).block();
		Profile returned1 = profileEndpoint.getUser(uid).block();
		assertEquals(ver1, returned1);
		
		Profile ver2 = new Profile("Dora", "Explorer", "she/her", "Female", List.of("Male"), 2000, "description here");
		profileEndpoint.updateUser(uid, ver2).block();
		Profile returned2 = profileEndpoint.getUser(uid).block();
		assertEquals(ver2, returned2);

		profileEndpoint.deleteUser(uid).block();
		assertThrows(RuntimeException.class, () -> profileEndpoint.getUser(uid).block());
	}

	@Test
	void contextLoads() {
	}

}
