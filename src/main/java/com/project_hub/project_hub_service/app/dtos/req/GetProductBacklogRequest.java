package com.project_hub.project_hub_service.app.dtos.req;

import java.util.List;

import com.project_hub.project_hub_service.app.constants.ProductBacklogPriority;
import com.project_hub.project_hub_service.app.constants.ProductBacklogStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetProductBacklogRequest {
    private String search;
    private ProductBacklogStatus status;
    private ProductBacklogPriority priority;
    private List<String> productGoalIds;
    private List<String> assigneeIds;
}
