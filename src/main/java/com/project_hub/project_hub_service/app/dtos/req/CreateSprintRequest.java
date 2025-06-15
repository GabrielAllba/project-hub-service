package com.project_hub.project_hub_service.app.dtos.req;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSprintRequest {

    @NotBlank
    private String projectId;
    @NotBlank
    private String name;
}
