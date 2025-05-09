package com.project_hub.project_hub_common_service.app.project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project_hub.project_hub_common_service.app.project.dtos.req.CreateProjectRequest;
import com.project_hub.project_hub_common_service.misc.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Projects", description = "Endpoints for managing projects")
@RestController
@RequestMapping("/api/projects")
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
}
