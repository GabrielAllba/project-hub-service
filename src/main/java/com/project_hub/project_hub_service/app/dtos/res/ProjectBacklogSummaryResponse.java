package com.project_hub.project_hub_service.app.dtos.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectBacklogSummaryResponse {
    private int totalTodo;
    private int totalInProgress;
    private int totalDone;
}
