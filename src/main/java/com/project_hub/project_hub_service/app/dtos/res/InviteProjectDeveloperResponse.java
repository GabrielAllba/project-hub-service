package com.project_hub.project_hub_service.app.dtos.res;

import java.time.LocalDateTime;

import com.project_hub.project_hub_service.app.constants.InvitationStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteProjectDeveloperResponse {
    private String invitationId;
    private String projectId;
    private String inviteeId;
    private String inviterId;
    private LocalDateTime invitedAt;
    private InvitationStatus status;
}
