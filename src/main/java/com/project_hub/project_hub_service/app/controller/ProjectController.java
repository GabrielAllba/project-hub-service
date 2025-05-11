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
import com.project_hub.project_hub_service.app.entity.Project;
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
    public ResponseEntity<BaseResponse<ProjectMember>> addMember(
            @PathVariable String projectId,
            @Validated @RequestBody AddMemberRequest dto) {

        ProjectMember member = projectUseCase.addMember(projectId, dto);

        BaseResponse<ProjectMember> response = new BaseResponse<>(
                "success",
                "Member invited successfully",
                member);

        return ResponseEntity.ok(response);
    }
}
