package com.project_hub.project_hub_service.app.dtos.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReorderProductBacklogRequest {
    @NotNull(message = "Active ID is required")
    private String activeId;

    private String originalContainer; // Can be null for product backlog

    private String currentContainer; // Can be null for product backlog

    @NotNull(message = "Insert position is required")
    @Min(value = 0, message = "Insert position must be non-negative")
    private Integer insertPosition;
}
