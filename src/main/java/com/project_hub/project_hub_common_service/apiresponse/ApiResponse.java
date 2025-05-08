package com.project_hub.project_hub_common_service.apiresponse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiResponse<T> {

    @Schema(description = "The status of the response", example = "success")
    private String status;

    @Schema(description = "The message providing additional details about the response", example = "Project created successfully")
    private String message;

    private T data;

    public ApiResponse(String status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }
}
