package com.miscsb.bubble.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.miscsb.bubble.api.proto.*;
import com.miscsb.bubble.controller.ProfileController;
import com.miscsb.bubble.service.adapter.ProtoAdapter;

import net.devh.boot.grpc.server.service.GrpcService;
import reactor.core.publisher.Mono;

@Component
@GrpcService
public class ProfileService extends ReactorProfileServiceGrpc.ProfileServiceImplBase {
    
    @Autowired
    ProfileController controller;

    @Override
    public Mono<CreateProfileResponse> createUser(Mono<CreateProfileRequest> request) {
        return request.map(CreateProfileRequest::getData)
                .map(ProtoAdapter::fromProto)
                .flatMap(controller::createUser)
                .map(Long::parseLong)
                .map(uid -> CreateProfileResponse.newBuilder().setUid(uid).build());
    }

    @Override
    public Mono<GetProfileResponse> getUser(Mono<GetProfileRequest> request) {
        return request.map(GetProfileRequest::getUid)
                .map(String::valueOf)
                .flatMap(controller::getUser)
                .map(ProtoAdapter::toProto)
                .map(profile -> GetProfileResponse.newBuilder().setData(profile).build());
    }

    @Override
    public Mono<UpdateProfileResponse> updateUser(Mono<UpdateProfileRequest> request) {
        return request.flatMap(
                req -> controller.updateUser(String.valueOf(req.getUid()), ProtoAdapter.fromProto(req.getData())))
                .map(_ -> UpdateProfileResponse.newBuilder().build());
    }

    @Override
    public Mono<DeleteProfileResponse> deleteUser(Mono<DeleteProfileRequest> request) {
        return request.map(DeleteProfileRequest::getUid)
                .map(String::valueOf)
                .flatMap(controller::deleteUser)
                .map(_ -> DeleteProfileResponse.newBuilder().build());
    }
}
