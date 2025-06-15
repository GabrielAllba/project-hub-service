package com.project_hub.project_hub_service.app.dtos.req;

import com.project_hub.project_hub_service.app.constants.ProductBacklogPriority;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EditBacklogPriorityRequest {
    @NotNull
    private String backlogId;

    @NotNull
    private ProductBacklogPriority priority;
}
