package com.miscsb.bubble.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.miscsb.bubble.api.proto.*;
import com.miscsb.bubble.controller.BubbleController;
import com.miscsb.bubble.service.adapter.ProtoAdapter;

import net.devh.boot.grpc.server.service.GrpcService;
import reactor.core.publisher.Mono;

@Component
@GrpcService
public class BubbleService extends ReactorBubbleServiceGrpc.BubbleServiceImplBase {
    
    @Autowired
    BubbleController controller;

    @Override
    public Mono<CreateBubbleResponse> createBubble(Mono<CreateBubbleRequest> request) {
        return request.map(CreateBubbleRequest::getData)
                .map(ProtoAdapter::fromProto)
                .flatMap(controller::createBubble)
                .map(Long::parseLong)
                .map(bid -> CreateBubbleResponse.newBuilder().setBid(bid).build());
    }

    @Override
    public Mono<GetBubbleResponse> getBubble(Mono<GetBubbleRequest> request) {
        return request.map(GetBubbleRequest::getBid)
                .map(String::valueOf)
                .flatMap(controller::getBubble)
                .map(ProtoAdapter::toProto)
                .map(bubble -> GetBubbleResponse.newBuilder().setData(bubble).build());
    }

    @Override
    public Mono<UpdateBubbleResponse> updateBubble(Mono<UpdateBubbleRequest> request) {
        return request.flatMap(
                req -> controller.updateBubble(String.valueOf(req.getBid()), ProtoAdapter.fromProto(req.getData())))
                .map(_ -> UpdateBubbleResponse.newBuilder().build());
    }

    @Override
    public Mono<DeleteBubbleResponse> deleteBubble(Mono<DeleteBubbleRequest> request) {
        return request.map(DeleteBubbleRequest::getBid)
                .map(String::valueOf)
                .flatMap(controller::deleteBubble)
                .map(_ -> DeleteBubbleResponse.newBuilder().build());
    }

    @Override
    public Mono<GetUserBubbleResponse> getUserBubble(Mono<GetUserBubbleRequest> request) {
        return request.map(GetUserBubbleRequest::getUid)
                .map(String::valueOf)
                .flatMap(controller::getUserBubble)
                .map(Long::parseLong)
                .map(bid -> GetUserBubbleResponse.newBuilder().setBid(bid).build());
    }

    @Override
    public Mono<SetUserBubbleResponse> setUserBubble(Mono<SetUserBubbleRequest> request) {
        return request.flatMap(req -> controller.setUserBubble(String.valueOf(req.getUid()), String.valueOf(req.getBid())))
                .map(_ -> SetUserBubbleResponse.newBuilder().build());
    }

    @Override
    public Mono<ResetUserBubbleResponse> resetUserBubble(Mono<ResetUserBubbleRequest> request) {
        return request.map(ResetUserBubbleRequest::getUid)
                .map(String::valueOf)
                .flatMap(controller::resetUserBubble)
                .map(_ -> ResetUserBubbleResponse.newBuilder().build());
    }
}
