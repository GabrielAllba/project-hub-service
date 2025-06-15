package com.project_hub.project_hub_service.app.usecase;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.project_hub.project_hub_service.app.constants.InvitationStatus;
import com.project_hub.project_hub_service.app.constants.ProjectRole;
import com.project_hub.project_hub_service.app.dtos.req.AddProductOwnerRequest;
import com.project_hub.project_hub_service.app.dtos.req.AddProjectDeveloperRequest;
import com.project_hub.project_hub_service.app.dtos.req.AddScrumMasterRequest;
import com.project_hub.project_hub_service.app.dtos.req.CreateProjectRequest;
import com.project_hub.project_hub_service.app.dtos.res.ProjectInvitationResponse;
import com.project_hub.project_hub_service.app.dtos.res.ProjectSummaryResponse;
import com.project_hub.project_hub_service.app.dtos.res.ProjectUserResponse;
import com.project_hub.project_hub_service.app.entity.Project;
import com.project_hub.project_hub_service.app.entity.ProjectDeveloper;
import com.project_hub.project_hub_service.app.entity.ProjectInvitation;
import com.project_hub.project_hub_service.app.entity.ProjectProductOwner;
import com.project_hub.project_hub_service.app.entity.ProjectScrumMaster;
import com.project_hub.project_hub_service.app.repository.gRpc.AuthenticationGrpcRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectDeveloperRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectInvitationRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectProductOwnerRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectScrumMasterRepository;

import authenticationservice.AuthenticationServiceOuterClass.FindUserResponse;

@Service
public class ProjectUseCase {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectDeveloperRepository projectDeveloperRepository;

    @Autowired
    private ProjectInvitationRepository projectInvitationRepository;

    @Autowired
    private AuthenticationGrpcRepository authenticationGrpcRepository;

    @Autowired
    private ProjectProductOwnerRepository projectProductOwnerRepository;

    @Autowired
    private ProjectScrumMasterRepository projectScrumMasterRepository;

    @Transactional
    public Project create(CreateProjectRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getPrincipal().toString();

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        Project saved = projectRepository.save(project);

        // Save the creator as product owner
        ProjectProductOwner owner = ProjectProductOwner.builder()
                .project(saved)
                .userId(userId)
                .build();
        projectProductOwnerRepository.save(owner);

        return saved;
    }

    public Project getProjectById(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project not found"));

        return project;
    }

    public List<ProjectInvitation> inviteDevelopers(String projectId, AddProjectDeveloperRequest request) {
        String requesterId = getCurrentUserId();
        Project project = getProjectOrThrow(projectId);

        if (!isProductOwnerOrScrumMaster(projectId, requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only product owner or scrum master can invite developers");
        }

        List<ProjectInvitation> invitations = request.getUserIds().stream().map(userId -> {
            if (requesterId.equals(userId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "You are already a member of this project");
            }

            FindUserResponse user = findUserOrThrow(userId);

            try {
                return ProjectInvitation.builder()
                        .invitedAt(LocalDateTime.now())
                        .status(InvitationStatus.PENDING)
                        .inviterId(requesterId)
                        .inviteeId(user.getId())
                        .project(project)
                        .role(ProjectRole.DEVELOPER)
                        .build();
            } catch (DataIntegrityViolationException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "User already invited: " + user.getUsername());
            }
        }).toList();

        return projectInvitationRepository.saveAll(invitations);
    }

    public List<ProjectInvitation> inviteProductOwners(String projectId, AddProductOwnerRequest request) {
        String requesterId = getCurrentUserId();
        Project project = getProjectOrThrow(projectId);

        if (!projectScrumMasterRepository.existsByProjectIdAndUserId(projectId, requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only a scrum master can invite product owners");
        }

        List<ProjectInvitation> invitations = request.getUserIds().stream().map(userId -> {
            if (requesterId.equals(userId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "You are already a member of this project");
            }

            FindUserResponse user = findUserOrThrow(userId);

            try {
                return ProjectInvitation.builder()
                        .invitedAt(LocalDateTime.now())
                        .status(InvitationStatus.PENDING)
                        .inviterId(requesterId)
                        .inviteeId(user.getId())
                        .project(project)
                        .role(ProjectRole.PRODUCT_OWNER)
                        .build();
            } catch (DataIntegrityViolationException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "User already invited: " + user.getUsername());
            }
        }).toList();

        return projectInvitationRepository.saveAll(invitations);
    }

    public List<ProjectInvitation> inviteScrumMasters(String projectId, AddScrumMasterRequest request) {
        String requesterId = getCurrentUserId();
        Project project = getProjectOrThrow(projectId);

        if (!projectProductOwnerRepository.existsByProjectIdAndUserId(projectId, requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the product owner can invite scrum masters");
        }

        List<ProjectInvitation> invitations = request.getUserIds().stream().map(userId -> {
            if (requesterId.equals(userId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are already a member of this project");
            }

            FindUserResponse user = findUserOrThrow(userId);

            try {
                return ProjectInvitation.builder()
                        .invitedAt(LocalDateTime.now())
                        .status(InvitationStatus.PENDING)
                        .inviterId(requesterId)
                        .inviteeId(user.getId())
                        .project(project)
                        .role(ProjectRole.SCRUM_MASTER)
                        .build();
            } catch (DataIntegrityViolationException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "User already invited: " + user.getUsername());
            }
        }).toList();

        return projectInvitationRepository.saveAll(invitations);
    }

    public ProjectInvitation acceptProjectInvitation(String invitationId) {
        String requesterId = getCurrentUserId();

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
                ProjectDeveloper newDev = ProjectDeveloper.builder()
                        .project(invitation.getProject())
                        .userId(requesterId)
                        .build();
                projectDeveloperRepository.save(newDev);
            } else if (invitation.getRole() == ProjectRole.SCRUM_MASTER) {
                projectScrumMasterRepository.save(ProjectScrumMaster.builder()
                        .project(invitation.getProject())
                        .userId(requesterId)
                        .build());
            } else if (invitation.getRole() == ProjectRole.PRODUCT_OWNER) {
                projectProductOwnerRepository.save(ProjectProductOwner.builder()
                        .project(invitation.getProject())
                        .userId(requesterId)
                        .build());
            }
            return invitation;
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are already a member of this project");
        }
    }

    public ProjectInvitation rejectProjectInvitation(String invitationId) {
        String requesterId = getCurrentUserId();

        ProjectInvitation invitation = projectInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));

        if (!invitation.getInviteeId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to reject this invitation");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation is no longer pending");
        }

        invitation.setStatus(InvitationStatus.REJECTED);
        invitation.setAcceptedAt(LocalDateTime.now());
        projectInvitationRepository.save(invitation);

        try {
            if (invitation.getRole() == ProjectRole.DEVELOPER) {
                ProjectDeveloper newDev = ProjectDeveloper.builder()
                        .project(invitation.getProject())
                        .userId(requesterId)
                        .build();
                projectDeveloperRepository.save(newDev);
            } else if (invitation.getRole() == ProjectRole.SCRUM_MASTER) {
                projectScrumMasterRepository.save(ProjectScrumMaster.builder()
                        .project(invitation.getProject())
                        .userId(requesterId)
                        .build());
            } else if (invitation.getRole() == ProjectRole.PRODUCT_OWNER) {
                projectProductOwnerRepository.save(ProjectProductOwner.builder()
                        .project(invitation.getProject())
                        .userId(requesterId)
                        .build());
            }
            return invitation;
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are already a member of this project");
        }
    }

    public Page<ProjectSummaryResponse> getProjectsForUser(Pageable pageable) {
        String userId = getCurrentUserId();

        List<ProjectDeveloper> devLinks = projectDeveloperRepository.findAllByUserId(userId);
        List<ProjectProductOwner> ownerLinks = projectProductOwnerRepository.findAllByUserId(userId);
        List<ProjectScrumMaster> scrumLinks = projectScrumMasterRepository.findAllByUserId(userId);

        Set<String> allProjectIds = new HashSet<>();
        Map<String, ProjectRole> roles = new HashMap<>();

        for (ProjectDeveloper pd : devLinks) {
            allProjectIds.add(pd.getProject().getId());
            roles.put(pd.getProject().getId(), ProjectRole.DEVELOPER);
        }

        for (ProjectProductOwner po : ownerLinks) {
            allProjectIds.add(po.getProject().getId());
            roles.put(po.getProject().getId(), ProjectRole.PRODUCT_OWNER);
        }

        for (ProjectScrumMaster sm : scrumLinks) {
            allProjectIds.add(sm.getProject().getId());
            roles.put(sm.getProject().getId(), ProjectRole.SCRUM_MASTER);
        }

        Page<Project> projects = projectRepository.findAllByIdIn(allProjectIds, pageable);

        return projects.map(p -> ProjectSummaryResponse.builder()
                .projectId(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .userRole(roles.get(p.getId()))
                .build());
    }

    public List<ProjectUserResponse> getProjectMembersByRole(String projectId, ProjectRole role) {
        List<String> userIds = switch (role) {
            case DEVELOPER -> projectDeveloperRepository.findAllByProjectId(projectId)
                    .stream().map(ProjectDeveloper::getUserId).toList();
            case SCRUM_MASTER -> projectScrumMasterRepository.findAllByProjectId(projectId)
                    .stream().map(ProjectScrumMaster::getUserId).toList();
            case PRODUCT_OWNER -> projectProductOwnerRepository.findAllByProjectId(projectId)
                    .stream().map(ProjectProductOwner::getUserId).toList();
        };

        return userIds.stream()
                .map(this::findUserOrThrow)
                .map(user -> ProjectUserResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .username(user.getUsername())
                        .role(role)
                        .build())
                .collect(Collectors.toList());
    }

    public Page<ProjectInvitationResponse> getProjectInvitationsByUserId(String userId, Pageable pageable) {
        Page<ProjectInvitation> invitations = projectInvitationRepository.findByInviteeId(userId, pageable);

        return invitations.map(inv -> ProjectInvitationResponse.builder()
                .id(inv.getId())
                .acceptedAt(inv.getAcceptedAt())
                .role(inv.getRole())
                .invitationId(inv.getId())
                .projectId(inv.getProject().getId())
                .inviterId(inv.getInviterId())
                .inviteeId(inv.getInviteeId())
                .status(inv.getStatus())
                .invitedAt(inv.getInvitedAt())
                .build());
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getPrincipal().toString();
    }

    private Project getProjectOrThrow(String projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    private FindUserResponse findUserOrThrow(String userId) {
        try {
            return authenticationGrpcRepository.findUser(userId);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User with id " + userId + " not found");
        }
    }

    private boolean isProductOwnerOrScrumMaster(String projectId, String userId) {
        return projectProductOwnerRepository.existsByProjectIdAndUserId(projectId, userId) ||
                projectScrumMasterRepository.existsByProjectIdAndUserId(projectId, userId);
    }
}
