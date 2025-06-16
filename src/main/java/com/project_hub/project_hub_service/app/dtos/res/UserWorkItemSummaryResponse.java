package com.project_hub.project_hub_service.app.dtos.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWorkItemSummaryResponse {
    private String email;
    private int todo;
    private int inProgress;
    private int done;
}
