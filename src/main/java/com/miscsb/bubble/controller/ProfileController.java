package com.miscsb.bubble.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.data.redis.support.collections.DefaultRedisList;
import org.springframework.data.redis.support.collections.RedisList;
import org.springframework.stereotype.Component;

import com.miscsb.bubble.KeyUtils;
import static com.miscsb.bubble.FunctionalUtils.*;
import com.miscsb.bubble.model.Profile;
import reactor.core.publisher.Mono;

@Component
public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    private final ReactiveRedisOperations<String, Profile> profileOps;
    private final RedisAtomicLong userIdCounter;
    private final RedisList<String> userIdList;
    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    public ProfileController(ReactiveRedisOperations<String, Profile> profileOps, StringRedisTemplate stringTemplate,
            ReactiveStringRedisTemplate reactiveStringRedisTemplate) {
        logger.info(getClass() + " instance initialized");
        this.profileOps = profileOps;
        this.userIdCounter = new RedisAtomicLong(KeyUtils.global("uid"), stringTemplate.getConnectionFactory());
        this.userIdList = new DefaultRedisList<String>(KeyUtils.global("users"), stringTemplate);
        this.reactiveStringRedisTemplate = reactiveStringRedisTemplate;
    }

    public Mono<String> createUser(Profile profile) {
        var create = Mono.fromCallable(userIdCounter::incrementAndGet)
                .map(String::valueOf)
                .flatMap(peek(userIdList::add));
        return create.flatMap(id -> {
            logger.info("Request to create profile " + profile + " at uid " + id);
            String gc = KeyUtils.genderChannelOut(profile.gender(), profile.preferredGenders());
            var m1 = profileOps.opsForValue().set(KeyUtils.uid(id), profile);
            var m2 = reactiveStringRedisTemplate.opsForSet().add(gc, id);
            return Mono.zip(m1, m2).map(constant(id));
        });
    }

    public Mono<Unit> updateUser(String id, Profile newProfile) {
        var guard = guard(profileOps::hasKey, () -> new RuntimeException("User ID does not exist"));
        return optional(Mono.just(KeyUtils.uid(id)).flatMap(guard)
                .map(KeyUtils::uid).flatMap(profileOps.opsForValue()::get)
                .flatMap(oldProfile -> {
                    logger.info("Request to update profile at uid " + id);
                    String gcOld = KeyUtils.genderChannelOut(oldProfile.gender(), oldProfile.preferredGenders());
                    String gcNew = KeyUtils.genderChannelOut(newProfile.gender(), newProfile.preferredGenders());
                    var m1 = profileOps.opsForValue().set(KeyUtils.uid(id), newProfile);
                    var m2 = reactiveStringRedisTemplate.opsForSet().remove(gcOld, id);
                    var m3 = reactiveStringRedisTemplate.opsForSet().add(gcNew, id);
                    return Mono.zip(m1, m2, m3).then();
                }));
    }

    public Mono<Profile> getUser(String id) {
        var guard = guard(profileOps::hasKey, () -> new RuntimeException("User ID does not exist"));
        return Mono.just(KeyUtils.uid(id)).flatMap(guard)
                .map(peek(logger::info))
                .flatMap(profileOps.opsForValue()::get);
    }
}
