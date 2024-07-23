package dev.miscsb.dating.endpoints;

import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.data.redis.support.collections.DefaultRedisList;
import org.springframework.data.redis.support.collections.DefaultRedisSet;
import org.springframework.data.redis.support.collections.RedisList;
import org.springframework.data.redis.support.collections.RedisSet;

import dev.hilla.runtime.transfertypes.Flux;
import dev.miscsb.dating.KeyUtils;
import dev.miscsb.dating.model.Bubble;
import dev.miscsb.dating.model.Profile;
import reactor.core.publisher.Mono;

public class MatchingEndpoint {
    private final ReactiveStringRedisTemplate stringTemplate;

    public MatchingEndpoint(ReactiveStringRedisTemplate stringTemplate) {
        this.stringTemplate = stringTemplate;
    }

    // public Flux<Profile> getMatches(String userId) {
    //     Mono<Profile> mp = reactiveStringTemplate.opsForValue().get(KeyUtils.uid(userId, "bubble")).flux().flatMap(bubbleId -> {
    //         String key = KeyUtils.bid(bubbleId, "members");
    //     });
    // }
}
