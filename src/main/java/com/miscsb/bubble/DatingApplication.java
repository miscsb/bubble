package com.miscsb.bubble;

import io.grpc.Server;
import net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
@ImportAutoConfiguration(GrpcServerAutoConfiguration.class)
public class DatingApplication {
	public static void main(String[] args) throws Exception {
		ApplicationContext context = SpringApplication.run(DatingApplication.class, args);
		Server grpcServer = context.getBean(Server.class);
		grpcServer.start();
		grpcServer.awaitTermination();
	}
}
