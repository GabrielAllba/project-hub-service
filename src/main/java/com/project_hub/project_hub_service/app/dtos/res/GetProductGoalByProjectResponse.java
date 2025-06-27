package com.project_hub.project_hub_service.app.dtos.res;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetProductGoalByProjectResponse {
    private String id;
    private String projectId;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int todoTask;
    private int inProgressTask;
    private int doneTask;
}
