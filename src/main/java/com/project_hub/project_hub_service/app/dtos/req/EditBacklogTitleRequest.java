package com.project_hub.project_hub_service.app.dtos.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EditBacklogTitleRequest {
    @NotNull
    private String backlogId;

    @NotBlank(message = "Title must not be blank")
    private String title;
}
