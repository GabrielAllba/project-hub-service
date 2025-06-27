package com.project_hub.project_hub_service.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project_hub.project_hub_service.app.dtos.res.BacklogActivityLogResponse;
import com.project_hub.project_hub_service.app.usecase.BacklogActivityLogUseCase;
import com.project_hub.project_hub_service.misc.BaseResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Logs", description = "Endpoints for managing and retrieving logs")
@RestController
@RequestMapping("/api/logs")
@SecurityRequirement(name = "bearerAuth")
public class ActivityLogController {

    @Autowired
    private BacklogActivityLogUseCase backlogActivityLogUseCase;

    @GetMapping("/backlog/{backlogId}")
    public ResponseEntity<BaseResponse<Page<BacklogActivityLogResponse>>> getMyActiveSprintActivities(
            @PathVariable String backlogId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<BacklogActivityLogResponse> activities = backlogActivityLogUseCase.getLogByBacklogId(backlogId, pageable);
        System.out.println("riel");
        System.out.println(activities);
        BaseResponse<Page<BacklogActivityLogResponse>> response = new BaseResponse<>(
                "success",
                "Your Backlog Logs retrieved successfully",
                activities);

        return ResponseEntity.ok(response);
    }
}
