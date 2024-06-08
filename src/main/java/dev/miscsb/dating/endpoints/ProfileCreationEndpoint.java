package dev.miscsb.dating.endpoints;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Component;

import com.vaadin.flow.server.auth.AnonymousAllowed;

import dev.hilla.Endpoint;
import dev.miscsb.dating.model.Profile;
import reactor.core.publisher.Mono;

@Endpoint
@AnonymousAllowed
@Component
public class ProfileCreationEndpoint {

    @Autowired
    private ReactiveRedisOperations<String, Profile> ops;

    public Mono<?> create(String userId, String firstName, String lastName, String pronouns, String gender, List<String> preferredGenders, int birthYear, String description) {
        Profile profile = new Profile(userId, firstName, lastName, pronouns, gender, preferredGenders, birthYear, description);
        return ops.opsForSet().add(userId, profile);
    }
}
