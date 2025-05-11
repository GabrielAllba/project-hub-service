package com.project_hub.project_hub_service.infrastructure.grpc;

import org.springframework.stereotype.Component;

import authenticationservice.AuthenticationServiceGrpc;
import authenticationservice.AuthenticationServiceOuterClass.ValidateTokenRequest;
import authenticationservice.AuthenticationServiceOuterClass.ValidateTokenResponse;


@Component
public class AuthenticationServiceGrpcClient {

    private final AuthenticationServiceGrpc.AuthenticationServiceBlockingStub stub;

    public AuthenticationServiceGrpcClient(AuthenticationServiceGrpc.AuthenticationServiceBlockingStub stub) {
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
