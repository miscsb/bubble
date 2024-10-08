package com.miscsb.bubble;

import static com.miscsb.bubble.GrpcTestUtil.*;

import java.util.List;

import com.miscsb.bubble.api.proto.*;
import com.miscsb.bubble.configuration.RedisConfig;
import com.miscsb.bubble.service.BubbleService;
import com.miscsb.bubble.service.MatchingService;
import com.miscsb.bubble.service.ProfileService;
import com.miscsb.bubble.service.adapter.ProtoAdapter;
import io.grpc.StatusException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.miscsb.bubble.model.Bubble;
import com.miscsb.bubble.model.Profile;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = { BubbleApplication.class, RedisConfig.class })
@Import(TestConfig.class)
public class GrpcServerServiceTests {

	@Autowired private BubbleService bubbleService;
	@Autowired private ProfileService profileService;
	@Autowired private MatchingService matchingService;

	@Test
	public void testMatchingSynchronous() throws StatusException {
		Bubble bubble1 = new Bubble("NY", 0.0, 0.0);
		Bubble bubble2 = new Bubble("SF", 10.0, 0.0);
		var createBubbleRequest1 = CreateBubbleRequest.newBuilder().setData(ProtoAdapter.toProto(bubble1)).build();
		var createBubbleRequest2 = CreateBubbleRequest.newBuilder().setData(ProtoAdapter.toProto(bubble2)).build();
		long bid1 = callOne(bubbleService::createBubble, createBubbleRequest1).getBid();
		long bid2 = callOne(bubbleService::createBubble, createBubbleRequest2).getBid();

		Profile profile1 = new Profile("John", "D.", "he/him", "Male", List.of("Female"), 2000, "Fun loving dude in the NYC area");
		Profile profile2 = new Profile("Jane", "D.", "she/her", "Female", List.of("Male"), 1999, "Fun loving girl in the NYC area");
		Profile profile3 = new Profile("Jake", "P.", "he/him", "Male", List.of("Female", "Male"), 1999, "Fun loving girl in the SF area");
		var createProfileRequest1 = CreateProfileRequest.newBuilder().setData(ProtoAdapter.toProto(profile1)).build();
		var createProfileRequest2 = CreateProfileRequest.newBuilder().setData(ProtoAdapter.toProto(profile2)).build();
		var createProfileRequest3 = CreateProfileRequest.newBuilder().setData(ProtoAdapter.toProto(profile3)).build();
		long uid1 = callOne(profileService::createUser, createProfileRequest1).getUid();
		long uid2 = callOne(profileService::createUser, createProfileRequest2).getUid();
		long uid3 = callOne(profileService::createUser, createProfileRequest3).getUid();

		var setUserBubbleRequest11 = SetUserBubbleRequest.newBuilder().setUid(uid1).setBid(bid1).build();
		var setUserBubbleRequest21 = SetUserBubbleRequest.newBuilder().setUid(uid2).setBid(bid1).build();
		var setUserBubbleRequest32 = SetUserBubbleRequest.newBuilder().setUid(uid3).setBid(bid2).build();
		callOne(bubbleService::setUserBubble, setUserBubbleRequest11);
		callOne(bubbleService::setUserBubble, setUserBubbleRequest21);
		callOne(bubbleService::setUserBubble, setUserBubbleRequest32);

		var getUserBubbleRequest1 = GetUserBubbleRequest.newBuilder().setUid(uid1).build();
		long retrievedBidForUid1 = callOne(bubbleService::getUserBubble, getUserBubbleRequest1).getBid();
		Assertions.assertEquals(retrievedBidForUid1, bid1);

		var getCandidatesRequest1 = GetCandidatesRequest.newBuilder().setUid(uid1).build();
		var candidatesForUid1 = callMany(matchingService::getCandidates, getCandidatesRequest1).stream().map(GetCandidatesResponse::getUid).toList();
		Assertions.assertEquals(candidatesForUid1, List.of(uid2));

		var setUserBubbleRequest4 = SetUserBubbleRequest.newBuilder().setUid(uid3).setBid(bid2).build();
		callOne(bubbleService::setUserBubble, setUserBubbleRequest4);
		Assertions.assertEquals(callOne(bubbleService::getUserBubble, getUserBubbleRequest1).getBid(), bid1);

		var getCandidatesRequest2 = GetCandidatesRequest.newBuilder().setUid(uid3).build();
		var candidatesForUid3 = callMany(matchingService::getCandidates, getCandidatesRequest2).stream().map(GetCandidatesResponse::getUid).toList();
		Assertions.assertEquals(candidatesForUid3, List.of());
	}

	@Test
	public void testBubbleCrudSynchronous() throws StatusException {
		// Test round-trip
		Bubble ver1 = new Bubble("PARTY CITY!", 0.0, 0.0);
		var createBubbleRequest = CreateBubbleRequest.newBuilder().setData(ProtoAdapter.toProto(ver1)).build();
		long bid = callOne(bubbleService::createBubble, createBubbleRequest).getBid();

		var getBubbleRequest1 = GetBubbleRequest.newBuilder().setBid(bid).build();
		Bubble returned1 = ProtoAdapter.fromProto(callOne(bubbleService::getBubble, getBubbleRequest1).getData());
		Assertions.assertEquals(ver1, returned1);

		// Test update
		Bubble ver2 = new Bubble("Doomed city", 0.0, 0.0);
		var updateBubbleRequest = UpdateBubbleRequest.newBuilder().setBid(bid).setData(ProtoAdapter.toProto(ver2)).build();
		callOne(bubbleService::updateBubble, updateBubbleRequest);

		var getBubbleRequest2 = GetBubbleRequest.newBuilder().setBid(bid).build();
		Bubble returned2 = ProtoAdapter.fromProto(callOne(bubbleService::getBubble, getBubbleRequest2).getData());
		Assertions.assertEquals(ver2, returned2);

		// Test delete
		var deleteBubbleRequest = DeleteBubbleRequest.newBuilder().setBid(bid).build();
		callOne(bubbleService::deleteBubble, deleteBubbleRequest);

		var getBubbleRequest3 = GetBubbleRequest.newBuilder().setBid(bid).build();
		Assertions.assertThrows(StatusException.class, () -> callOne(bubbleService::getBubble, getBubbleRequest3));
	}

	@Test
	public void testProfileCrudSynchronous() throws StatusException {
		// Test round-trip
		Profile ver1 = new Profile("Scrooge", "McDuck", "he/him", "Male", List.of("Female"), 1900, "$$$");
		var createProfileRequest = CreateProfileRequest.newBuilder().setData(ProtoAdapter.toProto(ver1)).build();
		long uid = callOne(profileService::createUser, createProfileRequest).getUid();

		var getProfileRequest1 = GetProfileRequest.newBuilder().setUid(uid).build();
		Profile returned1 = ProtoAdapter.fromProto(callOne(profileService::getUser, getProfileRequest1).getData());
		Assertions.assertEquals(ver1, returned1);

		// Test update
		Profile ver2 = new Profile("Dora", "Explorer", "she/her", "Female", List.of("Male"), 2000, "description here");
		var updateProfileRequest = UpdateProfileRequest.newBuilder().setUid(uid).setData(ProtoAdapter.toProto(ver2)).build();
		callOne(profileService::updateUser, updateProfileRequest);

		var getProfileRequest2 = GetProfileRequest.newBuilder().setUid(uid).build();
		Profile returned2 = ProtoAdapter.fromProto(callOne(profileService::getUser, getProfileRequest2).getData());
		Assertions.assertEquals(ver2, returned2);

		// Test delete
		var deleteProfileRequest = DeleteProfileRequest.newBuilder().setUid(uid).build();
		callOne(profileService::deleteUser, deleteProfileRequest);

		var getProfileRequest3 = GetProfileRequest.newBuilder().setUid(uid).build();
		Assertions.assertThrows(StatusException.class, () -> callOne(profileService::getUser, getProfileRequest3));
	}

	@Test
	public void testErrorOnDeleteProfileTwice() throws StatusException {
		Profile profile = new Profile("Scrooge", "McDuck", "he/him", "Male", List.of("Female"), 1900, "$$$");
		var createProfileRequest = CreateProfileRequest.newBuilder().setData(ProtoAdapter.toProto(profile)).build();
		long uid = callOne(profileService::createUser, createProfileRequest).getUid();

		var deleteProfileRequest = DeleteProfileRequest.newBuilder().setUid(uid).build();
		callOne(profileService::deleteUser, deleteProfileRequest);
		Assertions.assertThrows(StatusException.class, () -> callOne(profileService::deleteUser, deleteProfileRequest));
	}

	@Test
	public void testErrorOnDeleteBubbleTwice() throws StatusException {
		Bubble bubble = new Bubble("PARTY CITY!", 0.0, 0.0);
		var createBubbleRequest = CreateBubbleRequest.newBuilder().setData(ProtoAdapter.toProto(bubble)).build();
		long bid = callOne(bubbleService::createBubble, createBubbleRequest).getBid();

		var deleteBubbleRequest = DeleteBubbleRequest.newBuilder().setBid(bid).build();
		callOne(bubbleService::deleteBubble, deleteBubbleRequest);
		Assertions.assertThrows(StatusException.class, () -> callOne(bubbleService::deleteBubble, deleteBubbleRequest));
	}

	@Test
	public void testResetUserBubble() throws StatusException {
		Profile profile = new Profile("Scrooge", "McDuck", "he/him", "Male", List.of("Female"), 1900, "$$$");
		Bubble bubble = new Bubble("NY", 0.0, 0.0);
		var createProfileRequest = CreateProfileRequest.newBuilder().setData(ProtoAdapter.toProto(profile)).build();
		var createBubbleRequest = CreateBubbleRequest.newBuilder().setData(ProtoAdapter.toProto(bubble)).build();
		long uid = callOne(profileService::createUser, createProfileRequest).getUid();
		long bid = callOne(bubbleService::createBubble, createBubbleRequest).getBid();

		var setUserBubbleRequest = SetUserBubbleRequest.newBuilder().setUid(uid).setBid(bid).build();
		callOne(bubbleService::setUserBubble, setUserBubbleRequest);
		Assertions.assertEquals(callOne(bubbleService::getUserBubble, GetUserBubbleRequest.newBuilder().setUid(uid).build()).getBid(), bid);

		var resetUserBubbleRequest = ResetUserBubbleRequest.newBuilder().setUid(uid).build();
		callOne(bubbleService::resetUserBubble, resetUserBubbleRequest);
		var getUserBubbleRequest = GetUserBubbleRequest.newBuilder().setUid(uid).build();
		Assertions.assertEquals(callOne(bubbleService::getUserBubble, getUserBubbleRequest).getBid(), -1L);
	}

	@Test
	void contextLoads() {
	}

}
