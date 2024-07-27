// package com.miscsb.bubble.service;

// import static com.miscsb.bubble.FunctionalUtils.constant;

// import java.util.function.Function;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Component;

// import com.miscsb.bubble.KeyUtils;
// import com.miscsb.bubble.api.proto.ProfileCreationRequestDto;
// import com.miscsb.bubble.api.proto.ProfileCreationResponseDto;
// import com.miscsb.bubble.api.proto.ProfileDto;
// import com.miscsb.bubble.api.proto.ReactorProfileServiceGrpc;
// import com.miscsb.bubble.controller.ProfileController;
// import com.miscsb.bubble.model.Profile;

// import net.devh.boot.grpc.server.service.GrpcService;
// import reactor.core.publisher.Mono;
// import reactor.util.function.Tuple3;

// @Component
// @GrpcService
// public class ProfileService extends ReactorProfileServiceGrpc.ProfileServiceImplBase {

//     @Autowired
//     ProfileController controller;

//     public ProfileService(ProfileController controller) {
//         this.controller = controller;
//     }

//     static Profile toProfile(ProfileDto data, long id) {
//         return new Profile(String.valueOf(id), data.getFirstName(), data.getLastName(), data.getPronouns(),
//                 data.getGender(), data.getPreferredGendersList(), data.getBirthYear(), data.getDescription());
//     }

//     static ProfileDto toProfileDto(Profile profile) {
//         return ProfileDto.newBuilder()
//                 .setFirstName(profile.firstName())
//                 .setLastName(profile.lastName())
//                 .setPronouns(profile.pronouns())
//                 .setGender(profile.gender())
//                 .addAllPreferredGenders(profile.preferredGenders())
//                 .setBirthYear(profile.birthYear())
//                 .setDescription(profile.description())
//                 .build();

//     }

//     @Override
//     public Mono<ProfileCreationResponseDto> createUser(Mono<ProfileCreationRequestDto> request) {
//         return request.map(ProfileCreationRequestDto::getData).flatMap(profile -> {
//             // Two blocking calls, [TODO] is this correct?
//             Long id = userIdCounter.incrementAndGet();
//             userIdList.add(String.valueOf(id));

//             String gc = KeyUtils.genderChannelOut(profile.getGender(), profile.getPreferredGendersList());
//             var m1 = profileOps.opsForValue().set(KeyUtils.uid(id), profile);
//             var m2 = reactiveStringRedisTemplate.opsForSet().add(gc, String.valueOf(id));

//             ProfileCreationResponse response = ProfileCreationResponse.newBuilder().setId(id).build();
//             return Mono.zip(m1, m2).map(constant(response));
//         });
//     }

//     @Override
//     public Mono<ProfileUpdateResponse> updateUser(Mono<ProfileUpdateRequest> request) {
//         Function<ProfileUpdateRequest, Mono<Tuple3<Long, Profile, Profile>>> mapToProfileIfExists = data -> {
//             if (!userIdList.contains(String.valueOf(data.getId()))) {
//                 return Mono.error(new RuntimeException("Could not find this user."));
//             }
//             var m1 = profileOps.opsForValue().get(KeyUtils.uid(data.getId()));
//             return Mono.zip(Mono.just(data.getId()), m1, Mono.just(data.getData()));
//         };
//         return request.flatMap(mapToProfileIfExists).flatMap(tup -> {
//             Long id = tup.getT1();
//             Profile oldProfile = tup.getT2();
//             Profile newProfile = tup.getT3();

//             String gcOld = KeyUtils.genderChannelOut(oldProfile.getGender(), oldProfile.getPreferredGendersList());
//             String gcNew = KeyUtils.genderChannelOut(newProfile.getGender(), newProfile.getPreferredGendersList());

//             var m1 = profileOps.opsForValue().set(KeyUtils.uid(id), newProfile);
//             var m2 = reactiveStringRedisTemplate.opsForSet().remove(gcOld, String.valueOf(id));
//             var m3 = reactiveStringRedisTemplate.opsForSet().add(gcNew, String.valueOf(id));

//             ProfileUpdateResponse response = ProfileUpdateResponse.newBuilder().build();
//             return Mono.zip(m1, m2, m3).map(constant(response));
//         });
//     }

//     @Override
//     public Mono<ProfileGetResponse> getUser(Mono<ProfileGetRequest> request) {
//         return request
//                 .map(ProfileGetRequest::getId)
//                 .map(KeyUtils::uid)
//                 .flatMap(profileOps.opsForValue()::get)
//                 .map(profile -> ProfileGetResponse.newBuilder().setData(profile).build());
//     }
// }
