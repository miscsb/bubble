package com.miscsb.bubble.service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import com.miscsb.bubble.model.TwoProfileMatch;
import com.miscsb.bubble.model.TwoProfileMatchRedisSerializer;
import com.miscsb.redis.RedisCuckooFilter;
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
import org.springframework.data.redis.serializer.RedisSerializer;

@GrpcService
public class MatchingService extends MatchingServiceGrpc.MatchingServiceImplBase {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final StringRedisTemplate template;
    private final RedisTemplate<String, Profile> profileTemplate;
    private final RedisCuckooFilter cuckooFilter;
    private final RedisSerializer<TwoProfileMatch> serializer = TwoProfileMatchRedisSerializer.instance();

    public MatchingService(StringRedisTemplate template, RedisTemplate<String, Profile> profileTemplate, Function<String, RedisCuckooFilter> cfProvider) {
        this.template = template;
        this.profileTemplate = profileTemplate;
        this.cuckooFilter = cfProvider.apply("match_filter_prev");
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

            TwoProfileMatch pair = new TwoProfileMatch(String.valueOf(uid), result);
            if (cuckooFilter.exists(new String(Objects.requireNonNull(serializer.serialize(pair))))) continue;

            var response = GetCandidatesResponse.newBuilder().setUid(Long.parseLong(result)).build();
            responseObserver.onNext(response);
        }
        responseObserver.onCompleted();
    }

}
