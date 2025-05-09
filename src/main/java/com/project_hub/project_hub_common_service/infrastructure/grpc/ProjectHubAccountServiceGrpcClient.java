package com.project_hub.project_hub_common_service.infrastructure.grpc;

import org.springframework.stereotype.Component;

import projecthubaccount.ProjectHubAccountServiceGrpc;
import projecthubaccount.ProjectHubAccountServiceOuterClass.ValidateTokenRequest;
import projecthubaccount.ProjectHubAccountServiceOuterClass.ValidateTokenResponse;

@Component
public class ProjectHubAccountServiceGrpcClient {

    private final ProjectHubAccountServiceGrpc.ProjectHubAccountServiceBlockingStub stub;

    public ProjectHubAccountServiceGrpcClient(ProjectHubAccountServiceGrpc.ProjectHubAccountServiceBlockingStub stub) {
        this.stub = stub;
    }

    public ValidateTokenResponse validate(String token) {
        ValidateTokenRequest request = ValidateTokenRequest.newBuilder()
                .setToken(token)
                .build();
        ValidateTokenResponse response = stub.validateToken(request);
        return response;
    }
}
