package com.project_hub.project_hub_service.app.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.project_hub.project_hub_service.app.constants.ProjectRole;
import com.project_hub.project_hub_service.app.dtos.req.AddProductOwnerRequest;
import com.project_hub.project_hub_service.app.dtos.req.AddProjectDeveloperRequest;
import com.project_hub.project_hub_service.app.dtos.req.AddScrumMasterRequest;
import com.project_hub.project_hub_service.app.dtos.req.CreateProjectRequest;
import com.project_hub.project_hub_service.app.dtos.res.ProductBacklogResponse;
import com.project_hub.project_hub_service.app.dtos.res.ProjectBacklogSummaryResponse;
import com.project_hub.project_hub_service.app.dtos.res.ProjectInvitationResponse;
import com.project_hub.project_hub_service.app.dtos.res.ProjectSummaryResponse;
import com.project_hub.project_hub_service.app.dtos.res.ProjectUserResponse;
import com.project_hub.project_hub_service.app.dtos.res.SprintResponse;
import com.project_hub.project_hub_service.app.dtos.res.UserWorkItemSummaryResponse;
import com.project_hub.project_hub_service.app.entity.Project;
import com.project_hub.project_hub_service.app.entity.ProjectInvitation;
import com.project_hub.project_hub_service.app.usecase.ProductBacklogUseCase;
import com.project_hub.project_hub_service.app.usecase.ProjectUseCase;
import com.project_hub.project_hub_service.app.usecase.SprintUseCase;
import com.project_hub.project_hub_service.misc.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Projects", description = "Endpoints for managing projects")
@RestController
@RequestMapping("/api/project")
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

        @Autowired
        private ProjectUseCase projectUseCase;

        @Autowired
        private ProductBacklogUseCase productBacklogUseCase;

        @Autowired
        private SprintUseCase sprintUseCase;

        @PostMapping
        @Operation(summary = "Create a new project")
        public ResponseEntity<BaseResponse<ProjectSummaryResponse>> createProject(@Validated @RequestBody CreateProjectRequest dto) {
                ProjectSummaryResponse project = projectUseCase.create(dto);

                BaseResponse<ProjectSummaryResponse> response = new BaseResponse<>(
                                "success",
                                "Project created successfully",
                                project);

                return ResponseEntity.ok(response);
        }

        @GetMapping("/{projectId}")
        @Operation(summary = "Get a project by ID")
        public ResponseEntity<BaseResponse<ProjectSummaryResponse>> getProjectById(@PathVariable String projectId) {
                ProjectSummaryResponse project = projectUseCase.getProjectById(projectId);

                BaseResponse<ProjectSummaryResponse> response = new BaseResponse<>(
                                "success",
                                "Project retrieved successfully",
                                project);

                return ResponseEntity.ok(response);
        }

        @PostMapping("/{projectId}/developer/invite")
        @Operation(summary = "Invite developers to project")
        public ResponseEntity<BaseResponse<List<ProjectInvitationResponse>>> inviteDevelopers(
                        @PathVariable String projectId,
                        @Validated @RequestBody AddProjectDeveloperRequest dto) {

                List<ProjectInvitation> invitations = projectUseCase.inviteDevelopers(projectId, dto);

                List<ProjectInvitationResponse> responseData = invitations.stream()
                                .map(inv -> ProjectInvitationResponse.builder()
                                                .id(inv.getId())
                                                .acceptedAt(inv.getAcceptedAt())
                                                .role(inv.getRole())
                                                .invitationId(inv.getId())
                                                .projectId(inv.getProject().getId())
                                                .inviteeId(inv.getInviteeId())
                                                .inviterId(inv.getInviterId())
                                                .invitedAt(inv.getInvitedAt())
                                                .status(inv.getStatus())
                                                .build())
                                .toList();

                BaseResponse<List<ProjectInvitationResponse>> response = new BaseResponse<>(
                                "success",
                                "Developers invited successfully",
                                responseData);

                return ResponseEntity.ok(response);
        }

        @PostMapping("/{projectId}/product_owner/invite")
        @Operation(summary = "Invite product owners to project")
        public ResponseEntity<BaseResponse<List<ProjectInvitationResponse>>> inviteProductOwners(
                        @PathVariable String projectId,
                        @Validated @RequestBody AddProductOwnerRequest dto) {

                List<ProjectInvitation> invitations = projectUseCase.inviteProductOwners(projectId, dto);

                List<ProjectInvitationResponse> responseData = invitations.stream()
                                .map(inv -> ProjectInvitationResponse.builder()
                                                .id(inv.getId())
                                                .acceptedAt(inv.getAcceptedAt())
                                                .role(inv.getRole())
                                                .invitationId(inv.getId())
                                                .projectId(inv.getProject().getId())
                                                .inviteeId(inv.getInviteeId())
                                                .inviterId(inv.getInviterId())
                                                .invitedAt(inv.getInvitedAt())
                                                .status(inv.getStatus())
                                                .build())
                                .toList();

                BaseResponse<List<ProjectInvitationResponse>> response = new BaseResponse<>(
                                "success",
                                "Product owners invited successfully",
                                responseData);

                return ResponseEntity.ok(response);
        }

        @PostMapping("/{projectId}/scrum_master/invite")
        @Operation(summary = "Invite scrum masters to project")
        public ResponseEntity<BaseResponse<List<ProjectInvitationResponse>>> addScrumMasters(
                        @PathVariable String projectId,
                        @Validated @RequestBody AddScrumMasterRequest dto) {

                List<ProjectInvitation> invitations = projectUseCase.inviteScrumMasters(projectId, dto);

                List<ProjectInvitationResponse> responseData = invitations.stream()
                                .map(invitation -> ProjectInvitationResponse.builder()
                                                .id(invitation.getId())
                                                .acceptedAt(invitation.getAcceptedAt())
                                                .role(invitation.getRole())
                                                .invitationId(invitation.getId())
                                                .projectId(invitation.getProject().getId())
                                                .inviteeId(invitation.getInviteeId())
                                                .inviterId(invitation.getInviterId())
                                                .invitedAt(invitation.getInvitedAt())
                                                .status(invitation.getStatus())
                                                .build())
                                .toList();

                BaseResponse<List<ProjectInvitationResponse>> response = new BaseResponse<>(
                                "success",
                                "Scrum masters invited successfully",
                                responseData);

                return ResponseEntity.ok(response);
        }

        @PutMapping("/{invitationId}/accept")
        @Operation(summary = "Accept project invitation")
        public ResponseEntity<BaseResponse<ProjectInvitationResponse>> acceptInvitation(
                        @PathVariable String invitationId) {

                ProjectInvitation invitation = projectUseCase.acceptProjectInvitation(invitationId);
                ProjectInvitationResponse responseData = ProjectInvitationResponse
                                .builder()
                                .id(invitation.getId())
                                .acceptedAt(invitation.getAcceptedAt())
                                .role(invitation.getRole())
                                .invitationId(invitation.getId())
                                .projectId(invitation.getProject().getId())
                                .inviteeId(invitation.getInviteeId())
                                .inviterId(invitation.getInviterId())
                                .invitedAt(invitation.getInvitedAt())
                                .status(invitation.getStatus())
                                .build();
                BaseResponse<ProjectInvitationResponse> response = new BaseResponse<>(
                                "success",
                                "Invitation accepted successfully",
                                responseData);

                return ResponseEntity.ok(response);
        }

        @PutMapping("/{invitationId}/reject")
        @Operation(summary = "Reject project invitation")
        public ResponseEntity<BaseResponse<ProjectInvitationResponse>> rejectInvitation(
                        @PathVariable String invitationId) {

                ProjectInvitation invitation = projectUseCase.rejectProjectInvitation(invitationId);
                ProjectInvitationResponse responseData = ProjectInvitationResponse
                                .builder()
                                .id(invitation.getId())
                                .acceptedAt(invitation.getAcceptedAt())
                                .role(invitation.getRole())
                                .invitationId(invitation.getId())
                                .projectId(invitation.getProject().getId())
                                .inviteeId(invitation.getInviteeId())
                                .inviterId(invitation.getInviterId())
                                .invitedAt(invitation.getInvitedAt())
                                .status(invitation.getStatus())
                                .build();
                BaseResponse<ProjectInvitationResponse> response = new BaseResponse<>(
                                "success",
                                "Invitation rejected successfully",
                                responseData);

                return ResponseEntity.ok(response);
        }

        @GetMapping("/my")
        @Operation(summary = "Get paginated projects where the user is involved")
        public ResponseEntity<BaseResponse<Page<ProjectSummaryResponse>>> getMyProjects(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
                Page<ProjectSummaryResponse> projects = projectUseCase.getProjectsForUser(pageable);

                BaseResponse<Page<ProjectSummaryResponse>> response = new BaseResponse<>(
                                "success",
                                "Projects retrieved successfully",
                                projects);

                return ResponseEntity.ok(response);
        }

        @GetMapping("/{projectId}/sprints/all-status")
        public ResponseEntity<BaseResponse<Page<SprintResponse>>> getProjectSprintsAllStatus(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @PathVariable String projectId) {

                Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
                Page<SprintResponse> sprints = sprintUseCase.getPaginatedSprintsByProjectIdAllStatus(projectId,
                                pageable);

                BaseResponse<Page<SprintResponse>> response = new BaseResponse<>(
                                "success",
                                "Sprints retrieved successfully",
                                sprints);

                return ResponseEntity.ok(response);
        }

        @GetMapping("/{projectId}/sprints")
        @Operation(summary = "Get paginated sprints where the project is projectId")
        public ResponseEntity<BaseResponse<Page<SprintResponse>>> getProjectSprints(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @PathVariable String projectId) {

                Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
                Page<SprintResponse> sprints = sprintUseCase.getPaginatedSprintsByProjectId(projectId, pageable);

                BaseResponse<Page<SprintResponse>> response = new BaseResponse<>(
                                "success",
                                "Sprints retrieved successfully",
                                sprints);

                return ResponseEntity.ok(response);
        }

        @GetMapping("/{projectId}/sprints/timeline")
        @Operation(summary = "Get paginated timeline sprints for a given project and year")
        public ResponseEntity<BaseResponse<Page<SprintResponse>>> getProjectSprintsTimeline(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam int year, // 👈 NEW
                        @PathVariable String projectId) {

                Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").ascending());

                Page<SprintResponse> sprints = sprintUseCase.getPaginatedSprintsTimelineByProjectIdAndYear(projectId,
                                year, pageable);

                return ResponseEntity.ok(
                                new BaseResponse<>("success", "Sprints retrieved successfully", sprints));
        }

        @GetMapping("/{projectId}/product_backlogs")
        @Operation(summary = "Get paginated product backlog where the backlog is in project")
        public ResponseEntity<BaseResponse<Page<ProductBacklogResponse>>> getBacklogsByProjectPaginated(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @PathVariable String projectId) {
                Pageable pageable = PageRequest.of(page, size);
                Page<ProductBacklogResponse> productBacklogs = productBacklogUseCase.getPaginatedBacklogsByProjectId(
                                projectId,
                                pageable);

                BaseResponse<Page<ProductBacklogResponse>> response = new BaseResponse<>(
                                "success",
                                "Product backlog retrieved successfully",
                                productBacklogs);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/{projectId}/sprints/in_progress")
        @Operation(summary = "Get paginated in progress sprint in project")
        public ResponseEntity<BaseResponse<Page<SprintResponse>>> getInProgressSprintByProjectPaginated(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @PathVariable String projectId) {
                Pageable pageable = PageRequest.of(page, size);
                Page<SprintResponse> productBacklogs = sprintUseCase.getPaginatedInProgressSprintsByProjectId(
                                projectId,
                                pageable);

                BaseResponse<Page<SprintResponse>> response = new BaseResponse<>(
                                "success",
                                "Product backlog retrieved successfully",
                                productBacklogs);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/{projectId}/members")
        @Operation(summary = "Get project members by role")
        public ResponseEntity<BaseResponse<List<ProjectUserResponse>>> getProjectMembers(
                        @PathVariable String projectId,
                        @RequestParam("role") String role) {

                ProjectRole projectRole;
                try {
                        projectRole = ProjectRole.valueOf(role.toUpperCase());
                } catch (IllegalArgumentException e) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + role);
                }

                List<ProjectUserResponse> members = projectUseCase.getProjectMembersByRole(projectId, projectRole);
                BaseResponse<List<ProjectUserResponse>> response = new BaseResponse<>(
                                "success", "Project members retrieved", members);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/{projectId}/backlog_summary")
        public ResponseEntity<BaseResponse<ProjectBacklogSummaryResponse>> getProjectSummary(
                        @PathVariable String projectId) {
                ProjectBacklogSummaryResponse summary = productBacklogUseCase.getProjectBacklogSummary(projectId);
                BaseResponse<ProjectBacklogSummaryResponse> response = new BaseResponse<>(
                                "success", "Project backlog summary retrieved", summary);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/{projectId}/work_summary")
        public ResponseEntity<BaseResponse<List<UserWorkItemSummaryResponse>>> getWorkSummaryByProject(
                        @PathVariable String projectId,
                        @RequestParam(defaultValue = "7d") String range) {
                List<UserWorkItemSummaryResponse> summary = productBacklogUseCase
                                .getWorkSummaryByTeamAndDateRange(projectId, range);
                BaseResponse<List<UserWorkItemSummaryResponse>> response = new BaseResponse<>(
                                "success", "User work summary retrieved", summary);
                return ResponseEntity.ok(response);
        }

}
