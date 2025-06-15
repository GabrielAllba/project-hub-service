package com.project_hub.project_hub_service.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project_hub.project_hub_service.app.dtos.res.ProjectInvitationResponse;
import com.project_hub.project_hub_service.app.usecase.ProjectUseCase;
import com.project_hub.project_hub_service.misc.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Project Invitations", description = "Endpoints for managing projects")
@RestController
@RequestMapping("/api/project_invitation")
@SecurityRequirement(name = "bearerAuth")
public class ProjectInvitationController {

    @Autowired
    private ProjectUseCase projectUseCase;

    @GetMapping("/{userId}")
    @Operation(summary = "Get project invitations by user ID")
    public ResponseEntity<BaseResponse<Page<ProjectInvitationResponse>>> getProjectInvitationsByUserId(
            @PathVariable String userId,
            Pageable pageable) {

        Page<ProjectInvitationResponse> invitations = projectUseCase.getProjectInvitationsByUserId(userId, pageable);

        BaseResponse<Page<ProjectInvitationResponse>> response = new BaseResponse<>(
                "success",
                "Project invitations fetched successfully",
                invitations);

        return ResponseEntity.ok(response);
    }
}
