package com.project_hub.project_hub_service.app.dtos.req;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RenameProjectRequest {
    @NotNull
    private String projectId;

    @NotNull
    private String name;
}
