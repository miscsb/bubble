package com.miscsb.bubble.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.data.redis.support.collections.RedisList;

import com.miscsb.bubble.util.KeyUtils;
import com.miscsb.bubble.api.proto.*;
import com.miscsb.bubble.model.Profile;
import com.miscsb.bubble.service.adapter.ProtoAdapter;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class ProfileService extends ProfileServiceGrpc.ProfileServiceImplBase {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final RedisTemplate<String, Profile> profileTemplate;
    private final RedisAtomicLong userIdCounter;
    private final RedisList<String> userIdList;
    private final StringRedisTemplate template;
    
    public ProfileService(RedisTemplate<String, Profile> profileTemplate, RedisAtomicLong userIdCounter, RedisList<String> userIdList, StringRedisTemplate template) {
        this.profileTemplate = profileTemplate;
        this.userIdCounter = userIdCounter;
        this.userIdList = userIdList;
        this.template = template;
    }

    @Override
    public void createUser(CreateProfileRequest request, StreamObserver<CreateProfileResponse> responseObserver) {
        String uid = String.valueOf(userIdCounter.incrementAndGet());
        Profile profile = ProtoAdapter.fromProto(request.getData());
        logger.info("Request to create profile {} at uid {}", profile, uid);

        userIdList.add(uid);
        profileTemplate.opsForValue().set(KeyUtils.uid(uid), profile);

        var response = CreateProfileResponse.newBuilder().setUid(Long.parseLong(uid)).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void deleteUser(DeleteProfileRequest request, StreamObserver<DeleteProfileResponse> responseObserver) {
        String uid = String.valueOf(request.getUid());
        Profile profile = profileTemplate.opsForValue().get(KeyUtils.uid(uid));
        logger.info("Request to delete profile at uid {}", uid);
        if (profile == null) {
            responseObserver.onError(new StatusException(Status.NOT_FOUND));
            return;
        }

        userIdList.remove(uid);
        profileTemplate.delete(KeyUtils.uid(uid));

        String bid = template.opsForValue().getAndDelete(KeyUtils.uid(uid, "bubble"));
        if (bid != null) {
            String gc = KeyUtils.genderChannelOut(profile.gender(), profile.preferredGenders());
            template.opsForSet().remove(KeyUtils.bid(bid, "members"), uid);
            template.opsForSet().remove(KeyUtils.bid(bid, gc), uid);
        }

        var response = DeleteProfileResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getUser(GetProfileRequest request, StreamObserver<GetProfileResponse> responseObserver) {
        long uid = request.getUid();
        logger.info("Request to get profile at uid {}", uid);
        Profile profile = profileTemplate.opsForValue().get(KeyUtils.uid(uid));
        if (profile == null) {
            responseObserver.onError(new StatusException(Status.NOT_FOUND));
            return;
        }
        var response = GetProfileResponse.newBuilder().setData(ProtoAdapter.toProto(profile)).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void updateUser(UpdateProfileRequest request, StreamObserver<UpdateProfileResponse> responseObserver) {
        String uid = String.valueOf(request.getUid());
        Profile oldProfile = profileTemplate.opsForValue().get(KeyUtils.uid(uid));
        logger.info("Request to update profile at uid {}", uid);
        if (oldProfile == null) {
            responseObserver.onError(new StatusException(Status.NOT_FOUND));
            return;
        }
        Profile newProfile = ProtoAdapter.fromProto(request.getData());
        profileTemplate.opsForValue().set(KeyUtils.uid(uid), newProfile);

        String bid = template.opsForValue().get(KeyUtils.uid(uid, "bubble"));
        if (bid != null) {
            String gcOld = KeyUtils.genderChannelOut(oldProfile.gender(), oldProfile.preferredGenders());
            String gcNew = KeyUtils.genderChannelOut(newProfile.gender(), newProfile.preferredGenders());
            template.opsForSet().remove(KeyUtils.bid(bid, gcOld), uid);
            template.opsForSet().add(KeyUtils.bid(bid, gcNew), uid);
        }

        var response = UpdateProfileResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }    
}
