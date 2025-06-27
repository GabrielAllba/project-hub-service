package com.project_hub.project_hub_service.config;

import authenticationservice.AuthenticationServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfig {

    @Value("${grpc.auth-service.host}")
    private String authServiceHost;

    @Value("${grpc.auth-service.port}")
    private int authServicePort;

    @Bean
    public ManagedChannel authenticationServiceChannel() {
        return ManagedChannelBuilder.forAddress(authServiceHost, authServicePort)
                .usePlaintext()
                .build();
    }

    @Bean
    public AuthenticationServiceGrpc.AuthenticationServiceBlockingStub authenticationServiceBlockingStub(
            ManagedChannel authenticationServiceChannel) {
        return AuthenticationServiceGrpc.newBlockingStub(authenticationServiceChannel);
    }
}
