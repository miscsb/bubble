package com.miscsb.bubble.service;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.miscsb.bubble.util.KeyUtils;
import com.miscsb.bubble.api.proto.*;
import com.miscsb.bubble.model.Profile;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@Component
@GrpcService
public class MatchingService extends MatchingServiceGrpc.MatchingServiceImplBase {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final StringRedisTemplate template;
    private final RedisTemplate<String, Profile> profileTemplate;

    public MatchingService(StringRedisTemplate template, RedisTemplate<String, Profile> profileTemplate) {
        this.template = template;
        this.profileTemplate = profileTemplate;
    }

    @Override
    public void getMatches(GetMatchesRequest request, StreamObserver<GetMatchesResponse> responseObserver) {
        long uid = request.getUid();
        Profile profile = profileTemplate.opsForValue().get(KeyUtils.uid(uid));
        if (profile == null) {
            logger.info("No such user with uid {}", uid);
            responseObserver.onError(new StatusException(Status.NOT_FOUND));
            return;
        }
        // empty match list if the user does not have an associated bubble
        String bid = template.opsForValue().get(KeyUtils.uid(uid, "bubble"));
        logger.info("Request matches for uid {}, in bubble bid {}", uid, bid);
        if (bid == null) {
            responseObserver.onCompleted();
            return;
        }
        List<String> genderChannels = KeyUtils.genderChannelsIn(profile.gender(), profile.preferredGenders());
        String preferKey = KeyUtils.temp(uid, "match");
        String bubbleKey = KeyUtils.bid(bid, "members");
        template.opsForSet().unionAndStore(genderChannels, preferKey);
        
        Set<String> results = template.opsForSet().intersect(List.of(preferKey, bubbleKey));
        for (String result : Objects.requireNonNull(results)) {
            if (result.equals(String.valueOf(uid))) continue;
            var response = GetMatchesResponse.newBuilder().setUid(Long.parseLong(result)).build();
            responseObserver.onNext(response);
        }
        responseObserver.onCompleted();
    }
}
