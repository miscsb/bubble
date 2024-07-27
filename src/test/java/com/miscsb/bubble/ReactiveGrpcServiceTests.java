package com.miscsb.bubble;

import static org.junit.Assert.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.miscsb.bubble.api.proto.*;
import com.miscsb.bubble.configuration.RedisConfig;
import com.miscsb.bubble.model.Bubble;
import com.miscsb.bubble.service.BubbleService;
import com.miscsb.bubble.service.MatchingService;
import com.miscsb.bubble.service.ProfileService;
import com.miscsb.bubble.service.adapter.ProtoAdapter;

@SpringBootTest(classes = { DatingApplication.class, RedisConfig.class })
public class ReactiveGrpcServiceTests {

	@Autowired
	ProfileService profileService;
	@Autowired
	BubbleService bubbleService;
	@Autowired
	MatchingService matchingService;

	@Test
	@SuppressWarnings("unused")
	public void testBubbleCrudSynchronous() {
		Bubble ver1 = new Bubble("PARTY CITY!", 0.0, 0.0);
		CreateBubbleRequest request1 = CreateBubbleRequest.newBuilder().setData(ProtoAdapter.toProto(ver1)).build();
		long bid = bubbleService.createBubble(request1).block().getBid();

		GetBubbleRequest request2 = GetBubbleRequest.newBuilder().setBid(bid).build();
		Bubble returned1 = ProtoAdapter.fromProto(bubbleService.getBubble(request2).block().getData());
		assertEquals(ver1, returned1);

		Bubble ver2 = new Bubble("doomer city", 0.0, 0.0);
		UpdateBubbleRequest request3 = UpdateBubbleRequest.newBuilder().setBid(bid).setData(ProtoAdapter.toProto(ver2))
				.build();
		UpdateBubbleResponse response3 = bubbleService.updateBubble(request3).block();

		GetBubbleRequest request4 = GetBubbleRequest.newBuilder().setBid(bid).build();
		Bubble returned4 = ProtoAdapter.fromProto(bubbleService.getBubble(request4).block().getData());
		assertEquals(ver2, returned4);

		DeleteBubbleRequest request5 = DeleteBubbleRequest.newBuilder().setBid(bid).build();
		DeleteBubbleResponse response5 = bubbleService.deleteBubble(request5).block();

		GetBubbleRequest request6 = GetBubbleRequest.newBuilder().setBid(bid).build();
		assertThrows(RuntimeException.class, () -> bubbleService.getBubble(request6).block());
	}

}
