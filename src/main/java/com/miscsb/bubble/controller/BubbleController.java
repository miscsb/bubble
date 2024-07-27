package com.miscsb.bubble.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.data.redis.support.collections.*;
import org.springframework.stereotype.Component;

import static com.miscsb.bubble.FunctionalUtils.*;
import com.miscsb.bubble.KeyUtils;
import com.miscsb.bubble.model.Bubble;
import com.miscsb.bubble.model.Profile;
import reactor.core.publisher.Mono;

@Component
public class BubbleController {

    private static final Logger logger = LoggerFactory.getLogger(BubbleController.class);

    private final ReactiveRedisOperations<String, Bubble> bubbleOps;
    private final ReactiveRedisOperations<String, Profile> profileOps;
    private final RedisAtomicLong bubbleIdCounter;
    private final RedisList<String> bubbleIdList;
    private final ReactiveStringRedisTemplate reactiveStringTemplate;

    public BubbleController(ReactiveRedisOperations<String, Bubble> bubbleOps,
            ReactiveRedisOperations<String, Profile> profileOps,
            ReactiveStringRedisTemplate reactiveStringTemplate,
            StringRedisTemplate stringTemplate) {
        logger.info(getClass() + " instance initialized");
        this.bubbleOps = bubbleOps;
        this.profileOps = profileOps;
        this.bubbleIdCounter = new RedisAtomicLong(KeyUtils.global("bid"),
                stringTemplate.getConnectionFactory());
        this.bubbleIdList = new DefaultRedisList<String>(KeyUtils.global("bubbles"), stringTemplate);
        this.reactiveStringTemplate = reactiveStringTemplate;
    }

    public Mono<String> createBubble(Bubble bubble) {
        var create = Mono.defer(() -> Mono.just(bubbleIdCounter.incrementAndGet()))
                .map(String::valueOf) // Blocking call 1
                .flatMap(peek(bubbleIdList::add)); // Blocking call 2
        return create.flatMap(id -> {
            logger.info("Request to create bubble " + bubble + " at bid " + id);
            var m1 = bubbleOps.opsForValue().set(KeyUtils.bid(id), bubble);
            return m1.map(constant(id));
        });
    }

    public Mono<Unit> updateBubble(String id, Bubble bubble) {
        var guard = guard(bubbleOps::hasKey, () -> new RuntimeException("Bubble ID does not exist"));
        return optional(Mono.just(KeyUtils.bid(id)).flatMap(guard)
                .flatMap(peek(() -> logger.info("Request to update bubble at bid " + id)))
                .flatMap(key -> bubbleOps.opsForValue().set(key, bubble)));
    }

    public Mono<Bubble> getBubble(String id) {
        var guard = guard(bubbleOps::hasKey, () -> new RuntimeException("Bubble ID does not exist"));
        return Mono.just(KeyUtils.bid(id)).flatMap(guard)
                .flatMap(peek(logger::info))
                .flatMap(bubbleOps.opsForValue()::get);
    }

    public Mono<Unit> resetUserBubble(String userId) {
        return optional(Mono.just(KeyUtils.uid(userId, "bubble"))
                .filterWhen(reactiveStringTemplate::hasKey)
                .flatMap(peek(reactiveStringTemplate.opsForValue()::delete))
                .map(bid -> KeyUtils.bid(bid, "members"))
                .flatMap(peek(key -> reactiveStringTemplate.opsForSet().remove(key, userId))));
    }

    public Mono<Unit> attachUserToBubble(String userId, String bubbleId) {
        var guard1 = guard(bubbleOps::hasKey, () -> new RuntimeException("Bubble ID does not exist"));
        var m1 = Mono.just(KeyUtils.bid(bubbleId)).flatMap(guard1)
                .map(constant(KeyUtils.bid(bubbleId, "members")))
                .flatMap(key -> reactiveStringTemplate.opsForSet().add(key, userId));

        var guard2 = guard(profileOps::hasKey, () -> new RuntimeException("Profile ID does not exist"));
        var m2 = Mono.just(KeyUtils.uid(userId)).flatMap(guard2)
                .map(constant(KeyUtils.uid(userId, "bubble")))
                .flatMap(key -> reactiveStringTemplate.opsForValue().set(key, bubbleId));

        return Seq.start()
                .execMaybe(resetUserBubble(userId))
                .exec(Mono.zip(m1, m2))
                .exec(() -> logger.info("Attach uid " + userId + " to bid " + bubbleId))
                .end();
    }
}
