package com.project_hub.project_hub_service.app.repository.gRpc;

import org.springframework.stereotype.Repository;

import com.project_hub.project_hub_service.infrastructure.grpc.AuthenticationServiceGrpcClient;

import authenticationservice.AuthenticationServiceOuterClass.ValidateTokenResponse;

@Repository
public class AuthenticationGrpcRepository {

    private final AuthenticationServiceGrpcClient grpcClient;

    public AuthenticationGrpcRepository(AuthenticationServiceGrpcClient grpcClient) {
        this.grpcClient = grpcClient;
    }


     public ValidateTokenResponse validateToken(String token) {
        return grpcClient.validate(token);
    }
}
