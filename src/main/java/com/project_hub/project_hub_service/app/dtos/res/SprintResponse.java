package com.project_hub.project_hub_service.app.dtos.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintResponse {
    private String id;
    private String projectId;
    private String name;
    private String sprintGoal;
    private String startDate;
    private String endDate;
    private String createdAt;
    private String updatedAt;
    private String status;
}
