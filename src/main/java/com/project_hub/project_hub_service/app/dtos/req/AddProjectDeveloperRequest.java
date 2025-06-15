package com.project_hub.project_hub_service.app.dtos.req;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddProjectDeveloperRequest {

    private List<String> userIds;
}
