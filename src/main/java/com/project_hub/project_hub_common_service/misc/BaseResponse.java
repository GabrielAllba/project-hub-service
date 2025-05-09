package com.project_hub.project_hub_common_service.misc;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaseResponse<T> {

    private String status;

    private String message;

    private T data;

    public BaseResponse(String status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }
}
