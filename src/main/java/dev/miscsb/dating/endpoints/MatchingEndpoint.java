package dev.miscsb.dating.endpoints;

import java.util.List;

import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import dev.miscsb.dating.KeyUtils;
import dev.miscsb.dating.model.Profile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class MatchingEndpoint {
    private final ReactiveStringRedisTemplate template;
    private final ReactiveRedisOperations<String, Profile> profileOps;

    public MatchingEndpoint(ReactiveStringRedisTemplate template, ReactiveRedisOperations<String, Profile> profileOps) {
        this.template = template;
        this.profileOps = profileOps;
    }

    public Flux<String> getMatches(String userId) {
        var m1 = profileOps.opsForValue().get(KeyUtils.uid(userId));
        var m2 = template.opsForValue().get(KeyUtils.uid(userId, "bubble"));
        return Mono.zip(m1, m2).flux().flatMap(tup -> {
            List<String> genderChannels = KeyUtils.genderChannelsIn(tup.getT1().gender(), tup.getT1().preferredGenders());
            return template.opsForSet().union(genderChannels);
        });
    }
}
