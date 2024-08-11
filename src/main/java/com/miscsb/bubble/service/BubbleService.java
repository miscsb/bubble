package com.miscsb.bubble.service;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.data.redis.support.collections.RedisList;
import org.springframework.stereotype.Component;

import com.miscsb.bubble.util.KeyUtils;
import com.miscsb.bubble.api.proto.*;
import com.miscsb.bubble.model.Bubble;
import com.miscsb.bubble.service.adapter.ProtoAdapter;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@Component
@GrpcService
public class BubbleService extends BubbleServiceGrpc.BubbleServiceImplBase {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final RedisOperations<String, Bubble> bubbleTemplate;
    private final RedisAtomicLong bubbleIdCounter;
    private final RedisList<String> bubbleIdList;
    private final StringRedisTemplate template;

    public BubbleService(RedisOperations<String, Bubble> bubbleTemplate, RedisAtomicLong bubbleIdCounter, RedisList<String> bubbleIdList, StringRedisTemplate template) {
        this.bubbleTemplate = bubbleTemplate;
        this.bubbleIdCounter = bubbleIdCounter;
        this.bubbleIdList = bubbleIdList;
        this.template = template;
    }
    
    @Override
    public void createBubble(CreateBubbleRequest request, StreamObserver<CreateBubbleResponse> responseObserver) {
        String bid = String.valueOf(bubbleIdCounter.incrementAndGet());
        Bubble bubble = ProtoAdapter.fromProto(request.getData());
        logger.info("Request to create bubble {} at bid {}", bubble, bid);
        bubbleIdList.add(bid);
        bubbleTemplate.opsForValue().set(KeyUtils.bid(bid), bubble);

        var response = CreateBubbleResponse.newBuilder().setBid(Long.parseLong(bid)).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
    
    @Override
    public void deleteBubble(DeleteBubbleRequest request, StreamObserver<DeleteBubbleResponse> responseObserver) {
        long bid = request.getBid();
        logger.info("Request to delete bubble at bid {}", bid);
        if (!template.hasKey(KeyUtils.bid(bid))) {
            responseObserver.onError(new StatusException(Status.NOT_FOUND));
            return;
        }
        // Does not update the "bubble" field for users.
        // Instead, when a user's "bubble" field points to a nonexistent bubble, the field's key will be (lazily) deleted.
        template.delete(List.of(KeyUtils.bid(bid), KeyUtils.bid(bid, "members")));

        var response = DeleteBubbleResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getBubble(GetBubbleRequest request, StreamObserver<GetBubbleResponse> responseObserver) {
        long bid = request.getBid();
        logger.info("Request to get bubble at bid {}", bid);
        Bubble bubble = bubbleTemplate.opsForValue().get(KeyUtils.bid(bid));
        if (bubble == null) {
            responseObserver.onError(new StatusException(Status.NOT_FOUND));
            return;
        }
        var response = GetBubbleResponse.newBuilder().setData(ProtoAdapter.toProto(bubble)).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getUserBubble(GetUserBubbleRequest request, StreamObserver<GetUserBubbleResponse> responseObserver) {
        String uid = String.valueOf(request.getUid());
        logger.info("Request to get attached bubble at uid {}", uid);
        if (!template.hasKey(KeyUtils.uid(uid))) {
            responseObserver.onError(new StatusException(Status.NOT_FOUND));
            return;
        }
        String bid = template.opsForValue().get(KeyUtils.uid(uid, "bubble"));
        if (!template.hasKey(KeyUtils.bid(bid))) {
            logger.info("User with uid " + uid + " has attached bubble with " + bid + ", but this bubble does not exist. Resetting user bubble.");
            template.delete(KeyUtils.uid(uid, "bubble"));
            bid = null;
        }

        var builder = GetUserBubbleResponse.newBuilder();
        if (bid != null) builder.setBid(Long.parseLong(bid));
        var response = builder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void resetUserBubble(ResetUserBubbleRequest request, StreamObserver<ResetUserBubbleResponse> responseObserver) {
        String uid = String.valueOf(request.getUid());
        logger.info("Request to reset attached bubble at uid {}", uid);
        if (!template.hasKey(KeyUtils.uid(uid))) {
            responseObserver.onError(new StatusException(Status.NOT_FOUND));
            return;
        }
        String bid = template.opsForValue().getAndDelete(KeyUtils.uid(uid, "bubble"));
        template.opsForSet().remove(KeyUtils.bid(bid, "members"), uid);

        var response = ResetUserBubbleResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void setUserBubble(SetUserBubbleRequest request, StreamObserver<SetUserBubbleResponse> responseObserver) {
        String uid = String.valueOf(request.getUid());
        String bid = String.valueOf(request.getBid());
        logger.info("Request to set attached bubble at uid {} to bid {}", uid, bid);
        if (!template.hasKey(KeyUtils.bid(bid)) || !template.hasKey(KeyUtils.uid(uid))) {
            responseObserver.onError(new StatusException(Status.NOT_FOUND));
            return;
        }
        template.opsForValue().set(KeyUtils.uid(uid, "bubble"), bid);
        template.opsForSet().add(KeyUtils.bid(bid, "members"), uid);

        var response = SetUserBubbleResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
    
    @Override
    public void updateBubble(UpdateBubbleRequest request, StreamObserver<UpdateBubbleResponse> responseObserver) {
        long bid = request.getBid();
        Bubble bubble = ProtoAdapter.fromProto(request.getData());
        logger.info("Request to update bubble at bid {}", bid);
        if (!template.hasKey(KeyUtils.bid(bid))) {
            responseObserver.onError(new StatusException(Status.NOT_FOUND));
            return;
        }
        bubbleTemplate.opsForValue().set(KeyUtils.bid(bid), bubble);

        var response = UpdateBubbleResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
