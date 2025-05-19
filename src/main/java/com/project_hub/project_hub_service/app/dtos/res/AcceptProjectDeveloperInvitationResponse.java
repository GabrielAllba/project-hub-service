package com.project_hub.project_hub_service.app.dtos.res;

import com.project_hub.project_hub_service.app.constants.ProjectRole;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcceptProjectDeveloperInvitationResponse {
    private String projectId;
    private String userId;
    
    @Enumerated(EnumType.STRING)
    private ProjectRole role;
    
}
