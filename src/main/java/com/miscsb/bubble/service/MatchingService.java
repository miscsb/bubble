package com.miscsb.bubble.service;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.miscsb.bubble.model.TwoProfileMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.miscsb.bubble.util.KeyUtils;
import com.miscsb.bubble.api.proto.*;
import com.miscsb.bubble.model.Profile;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class MatchingService extends MatchingServiceGrpc.MatchingServiceImplBase {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final StringRedisTemplate template;
    private final RedisTemplate<String, Profile> profileTemplate;
    private final RedisTemplate<String, TwoProfileMatch> twoProfileMatchTemplate;

    public MatchingService(StringRedisTemplate template, RedisTemplate<String, Profile> profileTemplate, RedisTemplate<String, TwoProfileMatch> twoProfileMatchTemplate) {
        this.template = template;
        this.profileTemplate = profileTemplate;
        this.twoProfileMatchTemplate = twoProfileMatchTemplate;
    }

    @Override
    public void getCandidates(GetCandidatesRequest request, StreamObserver<GetCandidatesResponse> responseObserver) {
        long uid = request.getUid();
        Profile profile = profileTemplate.opsForValue().get(KeyUtils.uid(uid));
        if (profile == null) {
            logger.info("No such user with uid {}", uid);
            responseObserver.onError(new StatusException(Status.NOT_FOUND));
            return;
        }
        // empty match list if the user does not have an associated bubble
        String bid = template.opsForValue().get(KeyUtils.uid(uid, "bubble"));
        logger.info("Request candidates for uid {}, in bubble bid {}", uid, bid);
        if (bid == null) {
            responseObserver.onCompleted();
            return;
        }
        List<String> genderChannels = KeyUtils.genderChannelsIn(profile.gender(), profile.preferredGenders());
        List<String> gcKeys = genderChannels.stream().map(gc -> KeyUtils.bid(bid, gc)).toList();
        for (String result : Objects.requireNonNull(template.opsForSet().union(gcKeys))) {
            if (result.equals(String.valueOf(uid))) continue;
            var response = GetCandidatesResponse.newBuilder().setUid(Long.parseLong(result)).build();
            responseObserver.onNext(response);
        }
        responseObserver.onCompleted();
    }
}
