package com.miscsb.bubble.endpoints;

import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.data.redis.support.collections.*;
import org.springframework.web.bind.annotation.RestController;

import static com.miscsb.bubble.FunctionalUtils.*;
import com.miscsb.bubble.KeyUtils;
import com.miscsb.bubble.model.Bubble;
import com.miscsb.bubble.model.Profile;
import reactor.core.publisher.Mono;

@RestController
public class BubbleEndpoint {
    private final ReactiveRedisOperations<String, Bubble> bubbleOps;
    private final ReactiveRedisOperations<String, Profile> profileOps;
    private final RedisAtomicLong bubbleIdCounter;
    private final RedisList<String> bubbleIdList;
    private final ReactiveStringRedisTemplate reactiveStringTemplate;

    public BubbleEndpoint(ReactiveRedisOperations<String, Bubble> bubbleOps,
            ReactiveRedisOperations<String, Profile> profileOps, ReactiveStringRedisTemplate reactiveStringTemplate,
            StringRedisTemplate stringTemplate) {
        this.bubbleOps = bubbleOps;
        this.profileOps = profileOps;
        this.bubbleIdCounter = new RedisAtomicLong(KeyUtils.global("bid"), stringTemplate.getConnectionFactory());
        this.bubbleIdList = new DefaultRedisList<String>(KeyUtils.global("bubbles"), stringTemplate);
        this.reactiveStringTemplate = reactiveStringTemplate;
    }

    public Mono<String> createBubble(String bubbleName, double lat, double lon) {
        String id = String.valueOf(bubbleIdCounter.incrementAndGet());
        bubbleIdList.add(id);
        Bubble bubble = new Bubble(id, bubbleName, lat, lon, 0);
        return bubbleOps.opsForValue().set(KeyUtils.bid(id), bubble).flatMap(x -> Mono.just(id));
    }

    public Mono<Boolean> updateBubble(String id, String bubbleName, double lat, double lon) {
        if (!bubbleIdList.contains(id))
            return Mono.just(false);
        Bubble bubble = bubbleOps.opsForValue().get(KeyUtils.bid(id)).block();
        bubble = new Bubble(id, bubbleName, lat, lon, bubble.count());
        return bubbleOps.opsForValue().set(KeyUtils.bid(id), bubble).flatMap(x -> Mono.just(true));
    }

    public Mono<Bubble> getBubble(String id) {
        return bubbleOps.opsForValue().get(KeyUtils.bid(id));
    }

    public Mono<Boolean> attachUserToBubble(String userId, String bubbleId) {
        var mb = bubbleOps.opsForValue().get(KeyUtils.bid(bubbleId));
        var mp = profileOps.opsForValue().get(KeyUtils.uid(userId));
        var m1 = reactiveStringTemplate.opsForSet().add(KeyUtils.bid(bubbleId, "members"), userId);
        var m2 = reactiveStringTemplate.opsForValue().set(KeyUtils.uid(userId, "bubble"), bubbleId);
        return Mono.zip(mp, mb).then(Mono.zip(m1, m2).map(constant(true)));
    }
}
