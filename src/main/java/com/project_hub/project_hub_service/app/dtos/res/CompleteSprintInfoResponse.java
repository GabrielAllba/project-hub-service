package com.project_hub.project_hub_service.app.dtos.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteSprintInfoResponse {
    private int totalBacklogs;
    private int notDoneBacklogs;
    private int doneBacklogs;
}
