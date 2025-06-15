package com.project_hub.project_hub_service.app.dtos.req;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EditSprintGoalAndDatesRequest {

    @NotBlank
    private String sprintId;

    private String sprintGoal;
    private String startDate;
    private String endDate;
}
