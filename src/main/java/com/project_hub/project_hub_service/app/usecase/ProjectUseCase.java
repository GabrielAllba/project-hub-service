package com.project_hub.project_hub_service.app.usecase;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.project_hub.project_hub_service.app.dtos.req.AddMemberRequest;
import com.project_hub.project_hub_service.app.dtos.req.CreateProjectRequest;
import com.project_hub.project_hub_service.app.entity.InvitationStatus;
import com.project_hub.project_hub_service.app.entity.Project;
import com.project_hub.project_hub_service.app.entity.ProjectInvitation;
import com.project_hub.project_hub_service.app.entity.ProjectMember;
import com.project_hub.project_hub_service.app.repository.gRpc.AuthenticationGrpcRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectInvitationRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectMemberRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectRepository;

import authenticationservice.AuthenticationServiceOuterClass.FindUserResponse;

@Service
public class ProjectUseCase {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectInvitationRepository projectInvitationRepository;
    private final AuthenticationGrpcRepository authenticationGrpcRepository;

    public ProjectUseCase(AuthenticationGrpcRepository authenticationGrpcRepository,
            ProjectRepository projectRepository, ProjectInvitationRepository projectInvitationRepository,
            ProjectMemberRepository projectMemberRepository) {
        this.authenticationGrpcRepository = authenticationGrpcRepository;
        this.projectRepository = projectRepository;
        this.projectInvitationRepository = projectInvitationRepository;
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

    public ProjectInvitation inviteMember(String projectId, AddMemberRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String requesterId = authentication.getPrincipal().toString();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        if (!project.getCreatorId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the project creator can add members");
        }

        if (requesterId.equals(request.getUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The creator already associated with the member");
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
                    .build();

            projectInvitationRepository.save(invitation);
            return invitation;
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User already invited to this project");
        }
    }

    public ProjectMember acceptInvitation(String invitationId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getPrincipal().toString();

        ProjectInvitation invitation = projectInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));

        if (!invitation.getInviteeId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to accept this invitation");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation is no longer pending");
        }

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(LocalDateTime.now());
        projectInvitationRepository.save(invitation);

        Optional<ProjectMember> existingMember = projectMemberRepository
                .findByProjectIdAndUserId(invitation.getProject().getId(), userId);

        if (existingMember.isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are already a member of this project");
        }

        ProjectMember newMember = ProjectMember.builder()
                .project(invitation.getProject())
                .userId(userId)
                .build();

        return projectMemberRepository.save(newMember);
    }

}
