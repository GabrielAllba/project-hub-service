package com.project_hub.project_hub_common_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.grpc.ManagedChannelBuilder;
import projecthubaccount.ProjectHubAccountServiceGrpc;

@Configuration
public class GrpcConfig {

    @Bean
    public ProjectHubAccountServiceGrpc.ProjectHubAccountServiceBlockingStub projectHubAccountServiceBlockingStub() {
        return ProjectHubAccountServiceGrpc.newBlockingStub(
            ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build()
        );
    }
}
