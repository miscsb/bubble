package dev.miscsb.dating.endpoints;

import java.util.List;

import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.data.redis.support.collections.DefaultRedisList;
import org.springframework.data.redis.support.collections.RedisList;
import org.springframework.stereotype.Component;

import com.vaadin.flow.server.auth.AnonymousAllowed;

import dev.hilla.Endpoint;
import dev.miscsb.dating.model.Profile;
import reactor.core.publisher.Mono;

@Endpoint
@AnonymousAllowed
@Component
public class ProfileCreationEndpoint {
    
    private final ReactiveRedisOperations<String, Profile> profileOps;
    private final RedisAtomicLong userIdCounter;
    private final RedisList<String> userIdList;

    public ProfileCreationEndpoint(ReactiveRedisOperations<String, Profile> profileOps, StringRedisTemplate stringTemplate) {
        this.profileOps = profileOps;
        this.userIdCounter = new RedisAtomicLong("global:uid", stringTemplate.getConnectionFactory());
        this.userIdList = new DefaultRedisList<String>("global:users", stringTemplate);
    }
    
    public Mono<String> createUser(String firstName, String lastName, String pronouns, String gender, List<String> preferredGenders, int birthYear, String description) {
        String id = String.valueOf(userIdCounter.incrementAndGet());
        userIdList.add(id);
        Profile profile = new Profile(id, firstName, lastName, pronouns, gender, preferredGenders, birthYear, description);
        return profileOps.opsForValue().set("uid:" + id, profile).flatMap(x -> Mono.just(id));
    }

    public Mono<Boolean> updateUser(String id, String firstName, String lastName, String pronouns, String gender, List<String> preferredGenders, int birthYear, String description) {
        if (!userIdList.contains(id)) return Mono.just(false);
        Profile profile = new Profile(id, firstName, lastName, pronouns, gender, preferredGenders, birthYear, description);
        return profileOps.opsForValue().set("uid:" + id, profile).flatMap(x -> Mono.just(true));
    }

    public Mono<Profile> getUser(String id) {
        return profileOps.opsForValue().get("uid:" + id);
    }
}
