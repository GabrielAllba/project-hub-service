package com.project_hub.project_hub_common_service.app.project.dtos.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Response payload after creating a project")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProjectResponse {

    @Schema(description = "Unique ID of the project", example = "1")
    private String id;

    @Schema(description = "Project name", example = "My Awesome Project")
    private String name;
}

