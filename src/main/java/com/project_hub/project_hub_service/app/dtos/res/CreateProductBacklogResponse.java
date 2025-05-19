package com.project_hub.project_hub_service.app.dtos.res;

import com.project_hub.project_hub_service.app.constants.ProductBacklogPriority;
import com.project_hub.project_hub_service.app.constants.ProductBacklogStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductBacklogResponse {
    private String projectId;
    private String title;
    private ProductBacklogPriority priority;
    private ProductBacklogStatus status;
    private String creatorId;
}
