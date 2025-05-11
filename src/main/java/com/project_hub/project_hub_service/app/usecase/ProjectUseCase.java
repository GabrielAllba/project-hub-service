package com.project_hub.project_hub_service.app.usecase;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.project_hub.project_hub_service.app.dtos.req.AddMemberRequest;
import com.project_hub.project_hub_service.app.dtos.req.CreateProjectRequest;
import com.project_hub.project_hub_service.app.entity.Project;
import com.project_hub.project_hub_service.app.entity.ProjectMember;
import com.project_hub.project_hub_service.app.repository.gRpc.AuthenticationGrpcRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectMemberRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectRepository;

@Service
public class ProjectUseCase {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final AuthenticationGrpcRepository authenticationGrpcRepository;

    
    public ProjectUseCase( AuthenticationGrpcRepository authenticationGrpcRepository, ProjectRepository projectRepository, ProjectMemberRepository projectMemberRepository) {
        this.authenticationGrpcRepository = authenticationGrpcRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;

    }

    public Project create(CreateProjectRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getPrincipal().toString();

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .creatorId(userId)
                .build();

        Project saved = projectRepository.save(project);
        return saved;
    }

    public ProjectMember addMember(String projectId, AddMemberRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String requesterId = authentication.getPrincipal().toString();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        if (!project.getCreatorId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the project creator can add members");
        }

        ProjectMember member = ProjectMember.builder()
                .project(project)
                .userId(request.getUserId())
                .invitedAt(LocalDateTime.now())
                .build();

        return projectMemberRepository.save(member);
    }
}
