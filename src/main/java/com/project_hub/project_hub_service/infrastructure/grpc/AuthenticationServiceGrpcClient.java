package com.project_hub.project_hub_service.infrastructure.grpc;

import org.springframework.stereotype.Component;

import authenticationservice.AuthenticationServiceGrpc;
import authenticationservice.AuthenticationServiceOuterClass.FindUserRequest;
import authenticationservice.AuthenticationServiceOuterClass.FindUserResponse;
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

    public FindUserResponse findUser(String id){
         FindUserRequest request = FindUserRequest.newBuilder()
                .setId(id)
                .build();
        FindUserResponse response = stub.findUser(request);
        return response;
    }
}
