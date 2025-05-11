package com.project_hub.project_hub_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import authenticationservice.AuthenticationServiceGrpc;
import io.grpc.ManagedChannelBuilder;

@Configuration
public class GrpcConfig {

    @Bean
    public AuthenticationServiceGrpc.AuthenticationServiceBlockingStub authenticationServiceBlockingStub() {
        return AuthenticationServiceGrpc.newBlockingStub(
            ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build()
        );
    }
}
