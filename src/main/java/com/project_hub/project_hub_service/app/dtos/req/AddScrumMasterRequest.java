package com.project_hub.project_hub_service.app.dtos.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddScrumMasterRequest {

    @NotBlank(message = "User ID must not be blank")
    private String userId;
}
