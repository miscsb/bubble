package com.miscsb.bubble.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.miscsb.bubble.api.proto.*;
import com.miscsb.bubble.controller.MatchingController;

import net.devh.boot.grpc.server.service.GrpcService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@GrpcService
public class MatchingService extends ReactorMatchingServiceGrpc.MatchingServiceImplBase {

    @Autowired
    MatchingController controller;

    @Override
    public Flux<GetMatchesResponse> getMatches(Mono<GetMatchesRequest> request) {
        return request.flux().map(GetMatchesRequest::getUid)
                .map(String::valueOf)
                .flatMap(controller::getMatches)
                .map(Long::parseLong)
                .map(uid -> GetMatchesResponse.newBuilder().setUid(uid).build());
    }
}
