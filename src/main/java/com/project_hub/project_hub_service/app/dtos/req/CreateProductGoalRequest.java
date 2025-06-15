package com.project_hub.project_hub_service.app.dtos.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateProductGoalRequest{
    @NotBlank(message = "Title must not be blank")
    private String title;

    private String projectId;
}
