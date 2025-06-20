
package com.project_hub.project_hub_service.app.dtos.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTaskDistributionResponse {
    private String userId;
    private String name;
    private int totalTasks;
    private int doneTasks;
    private int inProgressTasks;
    private int todoTasks;
}
