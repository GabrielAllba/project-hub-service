package com.project_hub.project_hub_service.app.usecase;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.project_hub.project_hub_service.app.constants.InvitationStatus;
import com.project_hub.project_hub_service.app.constants.ProjectRole;
import com.project_hub.project_hub_service.app.dtos.req.AddProjectDeveloperRequest;
import com.project_hub.project_hub_service.app.dtos.req.AddScrumMasterRequest;
import com.project_hub.project_hub_service.app.dtos.req.CreateProjectRequest;
import com.project_hub.project_hub_service.app.dtos.res.ProjectSummaryResponse;
import com.project_hub.project_hub_service.app.entity.Project;
import com.project_hub.project_hub_service.app.entity.ProjectDeveloper;
import com.project_hub.project_hub_service.app.entity.ProjectInvitation;
import com.project_hub.project_hub_service.app.repository.gRpc.AuthenticationGrpcRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectDeveloperRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectInvitationRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectRepository;

import authenticationservice.AuthenticationServiceOuterClass.FindUserResponse;

@Service
public class ProjectUseCase {

    private final ProjectRepository projectRepository;
    private final ProjectDeveloperRepository projectDeveloperRepository;
    private final ProjectInvitationRepository projectInvitationRepository;
    private final AuthenticationGrpcRepository authenticationGrpcRepository;

    public ProjectUseCase(AuthenticationGrpcRepository authenticationGrpcRepository,
            ProjectRepository projectRepository, ProjectInvitationRepository projectInvitationRepository,
            ProjectDeveloperRepository projectDeveloperRepository) {
        this.authenticationGrpcRepository = authenticationGrpcRepository;
        this.projectRepository = projectRepository;
        this.projectInvitationRepository = projectInvitationRepository;
        this.projectDeveloperRepository = projectDeveloperRepository;

    }

    public Project create(CreateProjectRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getPrincipal().toString();

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .productOwnerId(userId)
                .build();

        Project saved = projectRepository.save(project);
        return saved;
    }

    public ProjectInvitation inviteDeveloper(String projectId, AddProjectDeveloperRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String requesterId = authentication.getPrincipal().toString();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        if (!project.getProductOwnerId().equals(requesterId) && !project.getScrumMasterId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the product owner and scrum master can add developers");
        }

        if (requesterId.equals(request.getUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You already be a product owner of this project");
        }
        if (requesterId.equals(project.getScrumMasterId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You already be a scrum master of this project");
        }

        FindUserResponse user;
        try {
            user = authenticationGrpcRepository.findUser(request.getUserId());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "User with id " + request.getUserId() + " not found");
        }
        try {
            ProjectInvitation invitation = ProjectInvitation.builder()
                    .invitedAt(LocalDateTime.now())
                    .status(InvitationStatus.PENDING)
                    .inviterId(requesterId)
                    .inviteeId(user.getId())
                    .project(project)
                    .role(ProjectRole.DEVELOPER)
                    .build();

            projectInvitationRepository.save(invitation);
            return invitation;
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User already invited to this project");
        }
    }

    public ProjectInvitation inviteScrumMaster(String projectId, AddScrumMasterRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String requesterId = authentication.getPrincipal().toString();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        if (!project.getProductOwnerId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the product owner can add scrum master");
        }

        if (requesterId.equals(request.getUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You already be a product owner of this project");
        }
        if (requesterId.equals(project.getScrumMasterId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You already be a scrum master of this project");
        }

        FindUserResponse user;
        try {
            user = authenticationGrpcRepository.findUser(request.getUserId());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "User with id " + request.getUserId() + " not found");
        }
        try {
            ProjectInvitation invitation = ProjectInvitation.builder()
                    .invitedAt(LocalDateTime.now())
                    .status(InvitationStatus.PENDING)
                    .inviterId(requesterId)
                    .inviteeId(user.getId())
                    .project(project)
                    .role(ProjectRole.SCRUM_MASTER)
                    .build();

            projectInvitationRepository.save(invitation);
            return invitation;
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User already invited to this project");
        }
    }

    public ProjectInvitation acceptProjectInvitation(String invitationId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String requesterId = authentication.getPrincipal().toString();

        ProjectInvitation invitation = projectInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));

        if (!invitation.getInviteeId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to accept this invitation");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation is no longer pending");
        }

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(LocalDateTime.now());
        projectInvitationRepository.save(invitation);

        try {
            if (invitation.getRole() == ProjectRole.DEVELOPER) {
                ProjectDeveloper newDeveloper = ProjectDeveloper.builder()
                        .project(invitation.getProject())
                        .userId(requesterId)
                        .build();
                projectDeveloperRepository.save(newDeveloper);
            } else if (invitation.getRole() == ProjectRole.SCRUM_MASTER) {
                Project project = projectRepository.findById(invitation.getProject().getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

                project.setScrumMasterId(requesterId);
                projectRepository.save(project);
            }
            return invitation;
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are already be a developer of this project");
        }
    }

    public Page<ProjectSummaryResponse> getProjectsForUser(Pageable pageable) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getPrincipal().toString();

        List<String> developerProjectIds = projectDeveloperRepository.findAllByUserId(userId)
                .stream()
                .map(pm -> pm.getProject().getId())
                .toList();

        List<String> productOwnerProjectIds = projectRepository.findAllByProductOwnerId(userId)
                .stream()
                .map(Project::getId)
                .toList();

        List<String> scrumMasterProjectIds = projectRepository.findAllByScrumMasterId(userId)
                .stream()
                .map(Project::getId)
                .toList();

        Set<String> allProjectIds = new HashSet<>();
        allProjectIds.addAll(developerProjectIds);
        allProjectIds.addAll(productOwnerProjectIds);
        allProjectIds.addAll(scrumMasterProjectIds);

        Page<Project> projectPage = projectRepository.findAllByIdIn(allProjectIds, pageable);

        return projectPage.map(project -> ProjectSummaryResponse.builder()
                .projectId(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .build());
    }

}
