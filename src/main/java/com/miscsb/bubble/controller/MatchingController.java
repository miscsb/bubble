package com.miscsb.bubble.controller;

import static com.miscsb.bubble.FunctionalUtils.*;

import java.util.List;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;

import com.miscsb.bubble.KeyUtils;
import com.miscsb.bubble.model.Profile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class MatchingController {

    private static final Logger logger = LoggerFactory.getLogger(MatchingController.class);

    private final ReactiveStringRedisTemplate template;
    private final ReactiveRedisOperations<String, Profile> profileOps;

    public MatchingController(ReactiveStringRedisTemplate template,
            ReactiveRedisOperations<String, Profile> profileOps) {
        logger.info(getClass() + " instance initialized");
        this.template = template;
        this.profileOps = profileOps;
    }

    public Flux<String> getMatches(String userId) {
        var guard1 = guard(template::hasKey, () -> new RuntimeException("User ID does not exist"));
        var m1 = Mono.just(KeyUtils.uid(userId))
                .flatMap(guard1)
                .flatMap(profileOps.opsForValue()::get);

        var guard2 = guard(template::hasKey, () -> new RuntimeException("User ID does not have an attached bubble"));
        var m2 = Mono.just(KeyUtils.uid(userId, "bubble"))
                .flatMap(guard2)
                .flatMap(template.opsForValue()::get);
        
        return Mono.zip(m1, m2).flux().flatMap(tup -> {
            logger.info("Request matches for uid " + userId);
            List<String> genderChannels = KeyUtils.genderChannelsIn(tup.getT1().gender(),
                    tup.getT1().preferredGenders());
            String preferKey = KeyUtils.temp(userId, "match");
            String bubbleKey = KeyUtils.bid(tup.getT2(), "members");
            var createSet = template.opsForSet().unionAndStore(genderChannels, preferKey).flux();
            return createSet.flatMap(setCount -> template.opsForSet().intersect(List.of(preferKey, bubbleKey)))
                    .filter(Predicate.not(userId::equals));
        });
    }
}
