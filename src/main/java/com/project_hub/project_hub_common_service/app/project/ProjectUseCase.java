package com.project_hub.project_hub_common_service.app.project;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.project_hub.project_hub_common_service.app.project.dtos.req.CreateProjectRequest;

@Service
public class ProjectUseCase {

    private final ProjectRepository projectRepository;


    public ProjectUseCase(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    
    public Project create(CreateProjectRequest request) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userId = authentication.getPrincipal().toString();
            
            Project project = Project.builder()
            .name(request.getName())
            .description(request.getDescription())
            .creatorId(userId)
            .build();
            
            Project saved  = projectRepository.save(project);
            
            return saved ;
    }    
}

