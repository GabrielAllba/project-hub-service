package com.project_hub.project_hub_common_service.app.project.dtos.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Request payload for creating a new project")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProjectRequest {

    @NotBlank
    @Schema(description = "Project name", example = "My Awesome Project")
    private String name;

    @Schema(description = "Optional project description", example = "A microservice-based project hub.")
    private String description;
}
