package com.project_hub.project_hub_common_service.app.project;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.project_hub.project_hub_common_service.app.project.dtos.req.CreateProjectRequest;
import com.project_hub.project_hub_common_service.app.token.TokenRepository;

import io.grpc.StatusRuntimeException;

@Service
public class ProjectUseCase {

    private final ProjectRepository projectRepository;
    private final TokenRepository tokenRepository;


    public ProjectUseCase(ProjectRepository projectRepository, TokenRepository tokenRepository) {
        this.projectRepository = projectRepository;
        this.tokenRepository = tokenRepository;
    }

    
    public Project create(CreateProjectRequest request, String authorizationHeader) {
            var validateTokenResponse = tokenRepository.validateToken(authorizationHeader);

            String userId = validateTokenResponse.getId();

            Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .creatorId(userId)
                .build();

            Project saved  = projectRepository.save(project);

            return saved ;
    }    
}

