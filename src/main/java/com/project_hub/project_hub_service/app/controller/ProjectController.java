package com.project_hub.project_hub_service.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project_hub.project_hub_service.app.dtos.req.AddProjectDeveloperRequest;
import com.project_hub.project_hub_service.app.dtos.req.AddScrumMasterRequest;
import com.project_hub.project_hub_service.app.dtos.req.CreateProductBacklogRequest;
import com.project_hub.project_hub_service.app.dtos.req.CreateProjectRequest;
import com.project_hub.project_hub_service.app.dtos.res.AcceptProjectDeveloperInvitationResponse;
import com.project_hub.project_hub_service.app.dtos.res.CreateProductBacklogResponse;
import com.project_hub.project_hub_service.app.dtos.res.InviteProjectDeveloperResponse;
import com.project_hub.project_hub_service.app.dtos.res.InviteScrumMasterResponse;
import com.project_hub.project_hub_service.app.dtos.res.ProductBacklogResponse;
import com.project_hub.project_hub_service.app.dtos.res.ProjectSummaryResponse;
import com.project_hub.project_hub_service.app.entity.ProductBacklog;
import com.project_hub.project_hub_service.app.entity.Project;
import com.project_hub.project_hub_service.app.entity.ProjectInvitation;
import com.project_hub.project_hub_service.app.usecase.ProductBacklogUseCase;
import com.project_hub.project_hub_service.app.usecase.ProjectUseCase;
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
        private final ProjectUseCase projectUseCase;
        private final ProductBacklogUseCase productBacklogUseCase;

        public ProjectController(ProjectUseCase projectUseCase, ProductBacklogUseCase productBacklogUseCase) {
                this.projectUseCase = projectUseCase;
                this.productBacklogUseCase = productBacklogUseCase;
        }

        @PostMapping
        @Operation(summary = "Create a new project")
        public ResponseEntity<BaseResponse<Project>> createProject(@Validated @RequestBody CreateProjectRequest dto) {
                Project project = projectUseCase.create(dto);

                BaseResponse<Project> response = new BaseResponse<>(
                                "success",
                                "Project created successfully",
                                project);

                return ResponseEntity.ok(response);
        }

        @PostMapping("/{projectId}/developer/invite")
        @Operation(summary = "Invite developer to project")
        public ResponseEntity<BaseResponse<InviteProjectDeveloperResponse>> addDeveloper(
                        @PathVariable String projectId,
                        @Validated @RequestBody AddProjectDeveloperRequest dto) {

                ProjectInvitation developer = projectUseCase.inviteDeveloper(projectId, dto);

                InviteProjectDeveloperResponse responseData = InviteProjectDeveloperResponse.builder()
                                .invitationId(developer.getId())
                                .projectId(developer.getProject().getId())
                                .inviteeId(developer.getInviteeId())
                                .inviterId(developer.getInviterId())
                                .invitedAt(developer.getInvitedAt())
                                .status(developer.getStatus())
                                .build();

                BaseResponse<InviteProjectDeveloperResponse> response = new BaseResponse<>(
                                "success",
                                "Developer invited successfully",
                                responseData);

                return ResponseEntity.ok(response);
        }

        @PostMapping("/{projectId}/scrum_master/invite")
        @Operation(summary = "Invite scrum master to project")
        public ResponseEntity<BaseResponse<InviteScrumMasterResponse>> addDeveloper(
                        @PathVariable String projectId,
                        @Validated @RequestBody AddScrumMasterRequest dto) {

                ProjectInvitation developer = projectUseCase.inviteScrumMaster(projectId, dto);

                InviteScrumMasterResponse responseData = InviteScrumMasterResponse.builder()
                                .invitationId(developer.getId())
                                .projectId(developer.getProject().getId())
                                .inviteeId(developer.getInviteeId())
                                .inviterId(developer.getInviterId())
                                .invitedAt(developer.getInvitedAt())
                                .status(developer.getStatus())
                                .build();

                BaseResponse<InviteScrumMasterResponse> response = new BaseResponse<>(
                                "success",
                                "Scrum master invited successfully",
                                responseData);

                return ResponseEntity.ok(response);
        }

        @PostMapping("/{invitationId}/accept")
        @Operation(summary = "Accept project invitation")
        public ResponseEntity<BaseResponse<AcceptProjectDeveloperInvitationResponse>> acceptInvitation(
                        @PathVariable String invitationId) {

                ProjectInvitation invitation = projectUseCase.acceptProjectInvitation(invitationId);
                AcceptProjectDeveloperInvitationResponse responseData = AcceptProjectDeveloperInvitationResponse
                                .builder()
                                .projectId(invitation.getProject().getId())
                                .userId(invitation.getInviteeId())
                                .role(invitation.getRole())
                                .build();
                BaseResponse<AcceptProjectDeveloperInvitationResponse> response = new BaseResponse<>(
                                "success",
                                "Invitation accepted successfully",
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

        @GetMapping("/{projectId}/product_backlog")
        @Operation(summary = "Get paginated product backlog where the backlog is in project")
        public ResponseEntity<BaseResponse<Page<ProductBacklogResponse>>> getBacklogsByProjectPaginated(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @PathVariable String projectId) {
                Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
                Page<ProductBacklogResponse> projects = productBacklogUseCase.getPaginatedBacklogsByProjectId(projectId,
                                pageable);

                BaseResponse<Page<ProductBacklogResponse>> response = new BaseResponse<>(
                                "success",
                                "Product backlog retrieved successfully",
                                projects);
                return ResponseEntity.ok(response);
        }

        @PostMapping("/{projectId}/product_backlog")
        @Operation(summary = "Create new product backlog in a project")
        public ResponseEntity<BaseResponse<CreateProductBacklogResponse>> createProductBacklog(
                        @Validated @RequestBody CreateProductBacklogRequest dto,
                        @PathVariable String projectId) {
                ProductBacklog productBacklog = productBacklogUseCase.create(projectId, dto);

                CreateProductBacklogResponse responseData = CreateProductBacklogResponse.builder()
                                .projectId(productBacklog.getProject().getId())
                                .title(productBacklog.getTitle())
                                .priority(productBacklog.getPriority())
                                .status(productBacklog.getStatus())
                                .creatorId(productBacklog.getCreatorId())
                                .build();
                BaseResponse<CreateProductBacklogResponse> response = new BaseResponse<>(
                                "success",
                                "Product backlog created successfully",
                                responseData);

                return ResponseEntity.ok(response);
        }
}
