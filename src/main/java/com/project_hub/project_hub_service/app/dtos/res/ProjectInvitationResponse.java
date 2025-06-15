package com.project_hub.project_hub_service.app.dtos.res;

import java.time.LocalDateTime;

import com.project_hub.project_hub_service.app.constants.InvitationStatus;
import com.project_hub.project_hub_service.app.constants.ProjectRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectInvitationResponse {
    private String id;
    private LocalDateTime invitedAt;
    private LocalDateTime acceptedAt;
    private InvitationStatus status;
    private String invitationId;
    private String inviterId;
    private String inviteeId;
    private String projectId;
    private ProjectRole role;

}
