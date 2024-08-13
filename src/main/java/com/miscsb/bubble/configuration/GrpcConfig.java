package com.miscsb.bubble.configuration;

import com.miscsb.bubble.service.BubbleService;
import com.miscsb.bubble.service.MatchingService;
import com.miscsb.bubble.service.ProfileService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;

@Primary @Configuration
public class GrpcConfig {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final int port;

    public GrpcConfig(@Value("${grpc.server.port}") int port) {
        this.port = port;
    }

    @Bean
    public Server grpcServer(ProfileService profileService, BubbleService bubbleService, MatchingService matchingService) throws IOException {
        logger.info("Starting grpc server on port {}", port);
        var protoReflectionService = ProtoReflectionService.newInstance();
        Server server = ServerBuilder.forPort(port)
                .addService(profileService)
                .addService(bubbleService)
                .addService(matchingService)
                .addService(protoReflectionService)
                .build();
        protoReflectionService.bindService();
        server.start();
        return server;
    }

}
