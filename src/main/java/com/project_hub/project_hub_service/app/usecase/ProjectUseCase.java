package com.project_hub.project_hub_service.app.usecase;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public ProjectSummaryResponse create(CreateProjectRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getPrincipal().toString();

        Project project = Project.builder()
                .name(request.getName())
                .build();

        Project saved = projectRepository.save(project);

        ProjectProductOwner owner = ProjectProductOwner.builder()
                .project(saved)
                .userId(userId)
                .build();
        projectProductOwnerRepository.save(owner);

        return ProjectSummaryResponse.builder()
                .projectId(saved.getId())
                .name(saved.getName())
                .userRole(ProjectRole.PRODUCT_OWNER) 
                .build();
    }

    public ProjectSummaryResponse getProjectById(String projectId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getPrincipal().toString();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        ProjectRole userRole = getUserRoleInProject(project, userId);

        return ProjectSummaryResponse.builder()
                .projectId(project.getId())
                .name(project.getName())
                .userRole(userRole)
                .build();
    }

    private ProjectRole getUserRoleInProject(Project project, String userId) {
        if (projectProductOwnerRepository.existsByProjectIdAndUserId(project.getId(), userId)) {
            return ProjectRole.PRODUCT_OWNER;
        } else if (projectScrumMasterRepository.existsByProjectIdAndUserId(project.getId(), userId)) {
            return ProjectRole.SCRUM_MASTER;
        } else if (projectDeveloperRepository.existsByProjectIdAndUserId(project.getId(), userId)) {
            return ProjectRole.DEVELOPER;
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this project");
        }
    }

    public List<ProjectInvitation> inviteDevelopers(String projectId, AddProjectDeveloperRequest request) {
        String requesterId = getCurrentUserId();
        Project project = getProjectOrThrow(projectId);

        if (!isProductOwnerOrScrumMaster(projectId, requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only a product owner or scrum master can invite developers");
        }

        List<ProjectInvitation> invitations = new ArrayList<>();
        Map<String, String> failedUsers = new LinkedHashMap<>();

        for (String userId : request.getUserIds()) {
            FindUserResponse user;
            try {
                user = findUserOrThrow(userId);
            } catch (ResponseStatusException e) {
                failedUsers.put(userId, "User not found");
                continue;
            }
            String identifier = user.getEmail();

            if (requesterId.equals(userId)) {
                failedUsers.put(identifier, "You are already a member of this project");
                continue;
            }

            if (isUserAlreadyMember(projectId, userId)) {
                failedUsers.put(identifier, "Already a member of this project");
                continue;
            }

            if (hasExistingInvitation(projectId, userId, ProjectRole.DEVELOPER)) {
                failedUsers.put(identifier, "Already invited as developer");
                continue;
            }

            Optional<ProjectInvitation> existingInvitation = getAnyExistingInvitation(projectId, userId);
            if (existingInvitation.isPresent()) {
                failedUsers.put(identifier, "Already invited as " +
                        existingInvitation.get().getRole().name().toLowerCase().replace("_", " "));
                continue;
            }

            invitations.add(ProjectInvitation.builder()
                    .invitedAt(LocalDateTime.now())
                    .status(InvitationStatus.PENDING)
                    .inviterId(requesterId)
                    .inviteeId(user.getId())
                    .project(project)
                    .role(ProjectRole.DEVELOPER)
                    .build());
        }

        if (!failedUsers.isEmpty()) {
            String errorMessage = failedUsers.entrySet().stream()
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining("; "));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to invite: " + errorMessage);
        }

        return projectInvitationRepository.saveAll(invitations);
    }

    public List<ProjectInvitation> inviteProductOwners(String projectId, AddProductOwnerRequest request) {
        String requesterId = getCurrentUserId();
        Project project = getProjectOrThrow(projectId);

        if (!projectProductOwnerRepository.existsByProjectIdAndUserId(projectId, requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only a product owner can invite other product owners");
        }

        List<ProjectInvitation> invitations = new ArrayList<>();
        Map<String, String> failedUsers = new LinkedHashMap<>();

        for (String userId : request.getUserIds()) {
            FindUserResponse user;
            try {
                user = findUserOrThrow(userId);
            } catch (ResponseStatusException e) {
                failedUsers.put(userId, "User not found");
                continue;
            }
            String identifier = user.getEmail();

            if (requesterId.equals(userId)) {
                failedUsers.put(identifier, "You are already a member of this project");
                continue;
            }

            if (isUserAlreadyMember(projectId, userId)) {
                failedUsers.put(identifier, "Already a member of this project");
                continue;
            }

            if (hasExistingInvitation(projectId, userId, ProjectRole.PRODUCT_OWNER)) {
                failedUsers.put(identifier, "Already invited as product owner");
                continue;
            }

            Optional<ProjectInvitation> existingInvitation = getAnyExistingInvitation(projectId, userId);
            if (existingInvitation.isPresent()) {
                failedUsers.put(identifier, "Already invited as " +
                        existingInvitation.get().getRole().name().toLowerCase().replace("_", " "));
                continue;
            }

            invitations.add(ProjectInvitation.builder()
                    .invitedAt(LocalDateTime.now())
                    .status(InvitationStatus.PENDING)
                    .inviterId(requesterId)
                    .inviteeId(user.getId())
                    .project(project)
                    .role(ProjectRole.PRODUCT_OWNER)
                    .build());
        }

        if (!failedUsers.isEmpty()) {
            String errorMessage = failedUsers.entrySet().stream()
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining("; "));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to invite: " + errorMessage);
        }

        return projectInvitationRepository.saveAll(invitations);
    }

    public List<ProjectInvitation> inviteScrumMasters(String projectId, AddScrumMasterRequest request) {
        String requesterId = getCurrentUserId();
        Project project = getProjectOrThrow(projectId);

        if (!isProductOwnerOrScrumMaster(projectId, requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only a product owner or scrum master can invite scrum masters");
        }

        List<ProjectInvitation> invitations = new ArrayList<>();
        Map<String, String> failedUsers = new LinkedHashMap<>();

        for (String userId : request.getUserIds()) {
            FindUserResponse user;
            try {
                user = findUserOrThrow(userId);
            } catch (ResponseStatusException e) {
                failedUsers.put(userId, "User not found");
                continue;
            }
            String identifier = user.getEmail();

            if (requesterId.equals(userId)) {
                failedUsers.put(identifier, "You are already a member of this project");
                continue;
            }

            if (isUserAlreadyMember(projectId, userId)) {
                failedUsers.put(identifier, "Already a member of this project");
                continue;
            }

            if (hasExistingInvitation(projectId, userId, ProjectRole.SCRUM_MASTER)) {
                failedUsers.put(identifier, "Already invited as scrum master");
                continue;
            }

            Optional<ProjectInvitation> existingInvitation = getAnyExistingInvitation(projectId, userId);
            if (existingInvitation.isPresent()) {
                failedUsers.put(identifier, "Already invited as " +
                        existingInvitation.get().getRole().name().toLowerCase().replace("_", " "));
                continue;
            }

            invitations.add(ProjectInvitation.builder()
                    .invitedAt(LocalDateTime.now())
                    .status(InvitationStatus.PENDING)
                    .inviterId(requesterId)
                    .inviteeId(user.getId())
                    .project(project)
                    .role(ProjectRole.SCRUM_MASTER)
                    .build());
        }

        if (!failedUsers.isEmpty()) {
            String errorMessage = failedUsers.entrySet().stream()
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining("; "));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to invite: " + errorMessage);
        }

        return projectInvitationRepository.saveAll(invitations);
    }

    @Transactional
    public ProjectInvitation acceptProjectInvitation(String invitationId) {
        String requesterId = getCurrentUserId();

        ProjectInvitation invitation = projectInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));

        if (!invitation.getInviteeId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, " not authorized to accept this invitation");
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, " already a member of this project");
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
        return projectInvitationRepository.save(invitation);
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

        // Cache inviter usernames
        Map<String, String> inviterUsernameMap = new HashMap<>();
        Set<String> inviterIds = invitations.stream()
                .map(ProjectInvitation::getInviterId)
                .collect(Collectors.toSet());

        for (String inviterId : inviterIds) {
            try {
                FindUserResponse inviterUser = findUserOrThrow(inviterId);
                inviterUsernameMap.put(inviterId, inviterUser.getUsername());
            } catch (Exception e) {
                inviterUsernameMap.put(inviterId, "Unknown");
            }
        }

        // Map to response
        return invitations.map(inv -> ProjectInvitationResponse.builder()
                .id(inv.getId())
                .acceptedAt(inv.getAcceptedAt())
                .role(inv.getRole())
                .invitationId(inv.getId())
                .projectId(inv.getProject().getId())
                .projectName(inv.getProject().getName())
                .inviterId(inv.getInviterId())
                .inviteeId(inv.getInviteeId())
                .inviterUsername(inviterUsernameMap.getOrDefault(inv.getInviterId(), "Unknown"))
                .status(inv.getStatus())
                .invitedAt(inv.getInvitedAt())
                .build());
    }

    public ProjectInvitationResponse getProjectInvitationById(String projectInvitationId) {
        Optional<ProjectInvitation> optionalInvitation = projectInvitationRepository.findById(projectInvitationId);
        if (optionalInvitation.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Invitation with id " + projectInvitationId + " not found");
        }

        ProjectInvitation invitation = optionalInvitation.get();

        String inviterUsername;
        try {
            FindUserResponse inviterUser = findUserOrThrow(invitation.getInviterId());
            inviterUsername = inviterUser.getUsername();
        } catch (Exception e) {
            inviterUsername = "Unknown";
        }

        return ProjectInvitationResponse.builder()
                .id(invitation.getId())
                .acceptedAt(invitation.getAcceptedAt())
                .role(invitation.getRole())
                .invitationId(invitation.getId())
                .projectId(invitation.getProject().getId())
                .projectName(invitation.getProject().getName())
                .inviterId(invitation.getInviterId())
                .inviteeId(invitation.getInviteeId())
                .inviterUsername(inviterUsername)
                .status(invitation.getStatus())
                .invitedAt(invitation.getInvitedAt())
                .build();
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

    private boolean isUserAlreadyMember(String projectId, String userId) {
        return projectScrumMasterRepository.existsByProjectIdAndUserId(projectId, userId)
                || projectDeveloperRepository.existsByProjectIdAndUserId(projectId, userId)
                || projectProductOwnerRepository.existsByProjectIdAndUserId(projectId, userId);
    }

    public boolean hasExistingInvitation(String projectId, String userId, ProjectRole role) {
        return projectInvitationRepository.existsByProjectIdAndInviteeIdAndRoleAndStatus(
                projectId, userId, role, InvitationStatus.PENDING);
    }

    public Optional<ProjectInvitation> getAnyExistingInvitation(String projectId, String userId) {
        return projectInvitationRepository
                .findFirstByProjectIdAndInviteeIdAndStatus(projectId, userId, InvitationStatus.PENDING);
    }

}
