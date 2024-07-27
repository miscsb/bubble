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

    private final ReactiveRedisOperations<String, Bubble> bubbles;
    private final RedisAtomicLong bubbleIdCounter;
    private final RedisList<String> bubbleIdList;
    private final ReactiveStringRedisTemplate template;

    public BubbleController(ReactiveRedisOperations<String, Bubble> bubbles,
            ReactiveRedisOperations<String, Profile> profiles,
            ReactiveStringRedisTemplate template,
            StringRedisTemplate templateSync) {
        logger.info(getClass() + " instance initialized");
        this.bubbles = bubbles;
        this.bubbleIdCounter = new RedisAtomicLong(KeyUtils.global("bid"), templateSync.getConnectionFactory());
        this.bubbleIdList = new DefaultRedisList<String>(KeyUtils.global("bubbles"), templateSync);
        this.template = template;
    }

    public Mono<String> createBubble(Bubble bubble) {
        var create = Mono.defer(() -> Mono.just(bubbleIdCounter.incrementAndGet()))
                .map(String::valueOf) // Blocking call 1
                .flatMap(peek(bubbleIdList::add)); // Blocking call 2
        return create.flatMap(id -> {
            logger.info("Request to create bubble " + bubble + " at bid " + id);
            var m1 = bubbles.opsForValue().set(KeyUtils.bid(id), bubble);
            return m1.map(constant(id));
        });
    }

    public Mono<Unit> updateBubble(String id, Bubble bubble) {
        var guard = guard(template::hasKey, () -> new RuntimeException("Bubble ID does not exist"));
        return Mono.just(KeyUtils.bid(id)).flatMap(guard)
                .flatMap(peek(() -> logger.info("Request to update bubble at bid " + id)))
                .flatMap(key -> bubbles.opsForValue().set(key, bubble)).map(Unit::instance);
    }

    public Mono<Unit> deleteBubble(String id) {
        var guard = guard(template::hasKey, () -> new RuntimeException("Bubble ID does not exist"));
        return Mono.just(KeyUtils.bid(id)).flatMap(guard)
                .flatMap(peek(() -> logger.info("Request to remove bubble at bid " + id)))
                .flatMap(key -> bubbles.opsForValue().delete(key)).map(Unit::instance);
    }

    public Mono<Bubble> getBubble(String id) {
        var guard = guard(template::hasKey, () -> new RuntimeException("Bubble ID does not exist"));
        return Mono.just(KeyUtils.bid(id)).flatMap(guard)
                .flatMap(peek(() -> logger.info("Request to get bubble at bid " + id)))
                .flatMap(bubbles.opsForValue()::get);
    }

    public Mono<String> getUserBubble(String userId) {
        var guard = guard(template::hasKey, () -> new RuntimeException("User ID does not exist"));
        return Mono.just(KeyUtils.uid(userId)).flatMap(guard)
                .flatMap(peek(() -> logger.info("Request to get attached bubble at uid " + userId)))
                .map(constant(KeyUtils.uid(userId, "bubble")))
                .flatMap(template.opsForValue()::get);
    }

    public Mono<Unit> resetUserBubble(String userId) {
        return optional(Mono.just(KeyUtils.uid(userId, "bubble"))
                .flatMap(peek(() -> logger.info("Request to reset attached bubble at uid " + userId)))
                .filterWhen(template::hasKey)
                .flatMap(peek(() -> logger.info("Bubble found at uid " + userId + ", resetting user bubble")))
                .flatMap(peek(template.opsForValue()::delete))
                .map(bid -> KeyUtils.bid(bid, "members"))
                .flatMap(peek(key -> template.opsForSet().remove(key, userId))));
    }

    public Mono<Unit> setUserBubble(String userId, String bubbleId) {
        var guard1 = guard(template::hasKey, () -> new RuntimeException("Bubble ID does not exist"));
        var m1 = Mono.just(KeyUtils.bid(bubbleId)).flatMap(guard1)
                .map(constant(KeyUtils.bid(bubbleId, "members")))
                .flatMap(key -> template.opsForSet().add(key, userId));

        var guard2 = guard(template::hasKey, () -> new RuntimeException("User ID does not exist"));
        var m2 = Mono.just(KeyUtils.uid(userId)).flatMap(guard2)
                .map(constant(KeyUtils.uid(userId, "bubble")))
                .flatMap(key -> template.opsForValue().set(key, bubbleId));

        return Seq.start()
                .execMaybe(resetUserBubble(userId))
                .exec(Mono.zip(m1, m2))
                .exec(() -> logger.info("Attach uid " + userId + " to bid " + bubbleId))
                .end();
    }
}
