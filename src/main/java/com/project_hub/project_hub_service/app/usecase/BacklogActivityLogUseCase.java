package com.project_hub.project_hub_service.app.usecase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.project_hub.project_hub_service.app.dtos.res.BacklogActivityLogResponse;
import com.project_hub.project_hub_service.app.entity.BacklogActivityLog;
import com.project_hub.project_hub_service.app.repository.gRpc.AuthenticationGrpcRepository;
import com.project_hub.project_hub_service.app.repository.postgres.BacklogActivityLogRepository;

import authenticationservice.AuthenticationServiceOuterClass.FindUserResponse;

@Service
public class BacklogActivityLogUseCase {

    @Autowired
    private BacklogActivityLogRepository backlogActivityLogRepository;

    @Autowired
    private AuthenticationGrpcRepository authenticationGrpcRepository;

    public Page<BacklogActivityLogResponse> getLogByBacklogId(String backlogId, Pageable pageable) {
        Page<BacklogActivityLog> activityLogsPage = backlogActivityLogRepository
                .findByBacklogIdOrderByCreatedAtDesc(backlogId, pageable);

        return activityLogsPage.map(this::toBacklogActivityResponse);
    }

    private BacklogActivityLogResponse toBacklogActivityResponse(BacklogActivityLog activityLog) {
        String username;
        try {
            FindUserResponse userResponse = findUserOrThrow(activityLog.getUserId());
            username = userResponse.getUsername();
        } catch (ResponseStatusException e) {
            username = activityLog.getUserId() != null ? activityLog.getUserId() : "Unknown User";
        }

        return BacklogActivityLogResponse.builder()
                .id(activityLog.getId())
                .backlogId(activityLog.getBacklog().getId())
                .userId(activityLog.getUserId())
                .username(username)
                .activityType(activityLog.getActivityType())
                .description(activityLog.getDescription())
                .oldValue(activityLog.getOldValue())
                .newValue(activityLog.getNewValue())
                .createdAt(activityLog.getCreatedAt())
                .build();
    }

    private FindUserResponse findUserOrThrow(String userId) {
        try {
            return authenticationGrpcRepository.findUser(userId);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "User with id " + userId + " not found", e); // Include original exception for better logging
        }
    }
}
