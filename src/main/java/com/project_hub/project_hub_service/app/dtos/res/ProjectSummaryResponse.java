package com.project_hub.project_hub_service.app.dtos.res;

import com.project_hub.project_hub_service.app.constants.ProjectRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSummaryResponse {
    private String projectId;
    private String name;
    private ProjectRole userRole; 
}
