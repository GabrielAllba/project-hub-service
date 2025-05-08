package com.project_hub.project_hub_common_service.app.token;

import org.springframework.stereotype.Repository;

import com.project_hub.project_hub_common_service.infrastructure.grpc.ProjectHubAccountServiceGrpcClient;

import projecthubaccount.ProjectHubAccountServiceOuterClass.ValidateTokenResponse;


@Repository
public class TokenRepository {

    private final ProjectHubAccountServiceGrpcClient projectHubAccountServiceGrpcClient;

    public TokenRepository(ProjectHubAccountServiceGrpcClient projectHubAccountServiceGrpcClient) {
        this.projectHubAccountServiceGrpcClient = projectHubAccountServiceGrpcClient;
    }

    public ValidateTokenResponse validateToken(String token) {
        return projectHubAccountServiceGrpcClient.validate(token);
    }
}
