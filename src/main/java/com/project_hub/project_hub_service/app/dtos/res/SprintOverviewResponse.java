package com.project_hub.project_hub_service.app.dtos.res;

import java.time.LocalDateTime;

import com.project_hub.project_hub_service.app.constants.SprintStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintOverviewResponse {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String sprintGoal;
    private SprintStatus status;
    private int totalTasks;
    private int completedTasks;
    private int totalPoints;
    private int completedPoints;
}
