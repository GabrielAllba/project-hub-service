package com.project_hub.project_hub_service.app.dtos.res;

import java.time.LocalDateTime;

import com.project_hub.project_hub_service.app.constants.ProductBacklogPriority;
import com.project_hub.project_hub_service.app.constants.ProductBacklogStatus;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductBacklogResponse {
    private String id;

    @Nullable
    private String prevBacklogId;
    private String projectId;
    private String sprintId;
    private String productGoalId;
    private int point;
    private String title;
    private ProductBacklogPriority priority;
    private ProductBacklogStatus status;
    private String creatorId;
    private String assigneeId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
