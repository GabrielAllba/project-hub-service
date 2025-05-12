package com.project_hub.project_hub_service.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project_hub.project_hub_service.app.dtos.req.AddMemberRequest;
import com.project_hub.project_hub_service.app.dtos.req.CreateProjectRequest;
import com.project_hub.project_hub_service.app.dtos.res.AcceptProjectInvitationResponse;
import com.project_hub.project_hub_service.app.dtos.res.InviteProjectMemberResponse;
import com.project_hub.project_hub_service.app.entity.Project;
import com.project_hub.project_hub_service.app.entity.ProjectInvitation;
import com.project_hub.project_hub_service.app.entity.ProjectMember;
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

    public ProjectController(ProjectUseCase projectUseCase) {
        this.projectUseCase = projectUseCase;
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

    @PostMapping("/{projectId}/member/invite")
    @Operation(summary = "Invite member to project")
    public ResponseEntity<BaseResponse<InviteProjectMemberResponse>> addMember(
            @PathVariable String projectId,
            @Validated @RequestBody AddMemberRequest dto) {

        ProjectInvitation member = projectUseCase.inviteMember(projectId, dto);

        InviteProjectMemberResponse responseData = InviteProjectMemberResponse.builder()
                .invitationId(member.getId())
                .projectId(member.getProject().getId())
                .inviteeId(member.getInviteeId())
                .inviterId(member.getInviterId())
                .invitedAt(member.getInvitedAt())
                .status(member.getStatus())
                .build();

        BaseResponse<InviteProjectMemberResponse> response = new BaseResponse<>(
                "success",
                "Member invited successfully",
                responseData);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{invitationId}/member/accept")
    @Operation(summary = "Accept project invitation")
    public ResponseEntity<BaseResponse<AcceptProjectInvitationResponse>> acceptInvitation(
            @PathVariable String invitationId) {

        ProjectMember member = projectUseCase.acceptInvitation(invitationId);
        AcceptProjectInvitationResponse responseData = AcceptProjectInvitationResponse.builder()
                .memberId(member.getId())
                .projectId(member.getProject().getId())
                .userId(member.getUserId())
                .build();
        BaseResponse<AcceptProjectInvitationResponse> response = new BaseResponse<>(
                "success",
                "Invitation accepted successfully",
                responseData);

        return ResponseEntity.ok(response);
    }
}
