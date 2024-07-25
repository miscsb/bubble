package com.miscsb.bubble.service;

import java.util.List;

import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.data.redis.support.collections.DefaultRedisList;
import org.springframework.data.redis.support.collections.RedisList;

import com.miscsb.bubble.KeyUtils;
import com.miscsb.bubble.api.proto.ProfileServiceGrpc;
import static com.miscsb.bubble.FunctionalUtils.*;
import com.miscsb.bubble.model.Profile;
import reactor.core.publisher.Mono;

public class ProfileService extends ProfileServiceGrpc.ProfileServiceImplBase {

    private final ReactiveRedisOperations<String, Profile> profileOps;
    private final RedisAtomicLong userIdCounter;
    private final RedisList<String> userIdList;
    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    public ProfileService(ReactiveRedisOperations<String, Profile> profileOps, StringRedisTemplate stringTemplate,
            ReactiveStringRedisTemplate reactiveStringRedisTemplate) {
        this.profileOps = profileOps;
        this.userIdCounter = new RedisAtomicLong(KeyUtils.global("uid"), stringTemplate.getConnectionFactory());
        this.userIdList = new DefaultRedisList<String>(KeyUtils.global("users"), stringTemplate);
        this.reactiveStringRedisTemplate = reactiveStringRedisTemplate;
    }

    public Mono<String> createUser(String firstName, String lastName, String pronouns, String gender,
            List<String> preferredGenders, int birthYear, String description) {
        String id = String.valueOf(userIdCounter.incrementAndGet());
        userIdList.add(id);
        Profile profile = new Profile(id, firstName, lastName, pronouns, gender, preferredGenders, birthYear,
                description);
        var m1 = profileOps.opsForValue().set(KeyUtils.uid(id), profile);
        var m2 = reactiveStringRedisTemplate.opsForSet().add(KeyUtils.genderChannelOut(gender, preferredGenders), id);
        return Mono.zip(m1, m2).map(constant(id));
    }

    public Mono<Boolean> updateUser(String id, String firstName, String lastName, String pronouns, String gender,
            List<String> preferredGenders, int birthYear, String description) {
        if (!userIdList.contains(id))
            return Mono.just(false);
        return profileOps.opsForValue().get(KeyUtils.uid(id)).flatMap(oldProfile -> {
            Profile profile = new Profile(id, firstName, lastName, pronouns, gender, preferredGenders, birthYear,
                    description);
            var m1 = profileOps.opsForValue().set(KeyUtils.uid(id), profile);
            var m2 = reactiveStringRedisTemplate.opsForSet().remove(KeyUtils.genderChannelOut(oldProfile.gender(), oldProfile.preferredGenders()), id);
            var m3 = reactiveStringRedisTemplate.opsForSet().add(KeyUtils.genderChannelOut(gender, preferredGenders), id);
            return Mono.zip(m1, m2, m3).map(constant(true));
        });
    }

    public Mono<Profile> getUser(String id) {
        return profileOps.opsForValue().get(KeyUtils.uid(id));
    }
}
