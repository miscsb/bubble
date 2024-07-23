package dev.miscsb.dating.endpoints;

import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.data.redis.support.collections.*;
import org.springframework.stereotype.Component;

import com.vaadin.flow.server.auth.AnonymousAllowed;

import dev.hilla.Endpoint;
import dev.miscsb.dating.model.Bubble;
import dev.miscsb.dating.model.Profile;
import reactor.core.publisher.Mono;


@Endpoint
@AnonymousAllowed
@Component
public class BubbleEndpoint {
    private final ReactiveRedisOperations<String, Bubble> bubbleOps;
    private final ReactiveRedisOperations<String, Profile> profileOps;
    private final RedisAtomicLong bubbleIdCounter;
    private final RedisList<String> bubbleIdList;
    private final StringRedisTemplate stringTemplate;

    public BubbleEndpoint(ReactiveRedisOperations<String, Bubble> bubbleOps, ReactiveRedisOperations<String, Profile> profileOps, StringRedisTemplate stringTemplate) {
        this.bubbleOps = bubbleOps;
        this.profileOps = profileOps;
        this.bubbleIdCounter = new RedisAtomicLong("global:bid", stringTemplate.getConnectionFactory());
        this.bubbleIdList = new DefaultRedisList<String>("global:bubbles", stringTemplate);
        this.stringTemplate = stringTemplate;
    }

    private RedisSet<String> bubbleMembers(String bubbleId) {
        return new DefaultRedisSet<>("bid:" + bubbleId + ":members:", stringTemplate);
    }

    public Mono<String> createBubble(String bubbleName, double lat, double lon) {
        String id = String.valueOf(bubbleIdCounter.incrementAndGet());
        bubbleIdList.add(id);
        Bubble bubble = new Bubble(id, bubbleName, lat, lon, 0);
        return bubbleOps.opsForValue().set("bid:" + id, bubble).flatMap(x -> Mono.just(id));
    }

    public Mono<Boolean> updateBubble(String id, String bubbleName, double lat, double lon) {
        if (!bubbleIdList.contains(id)) return Mono.just(false);
        Bubble bubble = bubbleOps.opsForValue().get("bid:" + id).block();
        bubble = new Bubble(id, bubbleName, lat, lon, bubble.count());
        return bubbleOps.opsForValue().set("bid:" + id, bubble).flatMap(x -> Mono.just(true));
    }

    public Mono<Bubble> getBubble(String id) {
        return bubbleOps.opsForValue().get("bid:" + id);
    }

    public Mono<Boolean> attachUserToBubble(String userId, String bubbleId) {
        Mono<Bubble>  mb = bubbleOps .opsForValue().get("bid:" + bubbleId);
        Mono<Profile> mp = profileOps.opsForValue().get("uid:" + userId);
        return mp.flatMap(profile -> mb.map(bubble -> {
            bubbleMembers(bubbleId).add(userId);
            stringTemplate.boundValueOps("uid:" + userId + ":bubble").set(bubbleId);
            System.out.println("SET");
            return true;
        }));
    }
}
