package com.project_hub.project_hub_service.app.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project_hub.project_hub_service.app.constants.ProductBacklogStatus;
import com.project_hub.project_hub_service.app.dtos.req.CreateSprintRequest;
import com.project_hub.project_hub_service.app.dtos.req.EditSprintGoalAndDatesRequest;
import com.project_hub.project_hub_service.app.dtos.res.CompleteSprintInfoResponse;
import com.project_hub.project_hub_service.app.dtos.res.ProductBacklogResponse;
import com.project_hub.project_hub_service.app.dtos.res.SprintResponse;
import com.project_hub.project_hub_service.app.entity.ProductBacklog;
import com.project_hub.project_hub_service.app.usecase.ProductBacklogUseCase;
import com.project_hub.project_hub_service.app.usecase.SprintUseCase;
import com.project_hub.project_hub_service.misc.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Sprints", description = "Endpoints for managing sprints")
@RestController
@RequestMapping("/api/sprint")
@SecurityRequirement(name = "bearerAuth")
public class SprintController {

        private final SprintUseCase sprintUseCase;
        private final ProductBacklogUseCase productBacklogUseCase;

        @Autowired
        public SprintController(SprintUseCase sprintUseCase, ProductBacklogUseCase productBacklogUseCase) {
                this.sprintUseCase = sprintUseCase;
                this.productBacklogUseCase = productBacklogUseCase;
        }

        @PostMapping
        @Operation(summary = "Create a new sprint")
        public ResponseEntity<BaseResponse<SprintResponse>> createSprint(
                        @Validated @RequestBody CreateSprintRequest dto) {
                SprintResponse sprint = sprintUseCase.create(dto);

                BaseResponse<SprintResponse> baseResponse = new BaseResponse<>(
                                "success",
                                "Sprint successfully created",
                                sprint);

                return ResponseEntity.ok(baseResponse);
        }

        @GetMapping("/{sprintId}/product_backlogs")
        @Operation(summary = "Get paginated product backlog where the backlog is in sprint")
        public ResponseEntity<BaseResponse<Page<ProductBacklogResponse>>> getBacklogsByProjectPaginated(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @PathVariable String sprintId) {
                Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
                Page<ProductBacklogResponse> productBacklogs = productBacklogUseCase.getPaginatedBacklogsBySprintId(
                                sprintId,
                                pageable);

                BaseResponse<Page<ProductBacklogResponse>> response = new BaseResponse<>(
                                "success",
                                "Product backlog retrieved successfully",
                                productBacklogs);
                return ResponseEntity.ok(response);
        }

        @PutMapping("/edit_goal_and_dates")
        @Operation(summary = "Edit sprint goal, start date, and end date")
        public ResponseEntity<BaseResponse<SprintResponse>> editSprintGoalAndDates(
                        @Validated @RequestBody EditSprintGoalAndDatesRequest dto) {

                SprintResponse updatedSprint = sprintUseCase.editGoalAndDates(dto);

                BaseResponse<SprintResponse> response = new BaseResponse<>(
                                "success",
                                "Sprint goal and dates updated successfully",
                                updatedSprint);

                return ResponseEntity.ok(response);
        }

        @GetMapping("/{sprintId}")
        @Operation(summary = "Get a sprint by ID")
        public ResponseEntity<BaseResponse<SprintResponse>> getSprintById(@PathVariable String sprintId) {
                SprintResponse sprint = sprintUseCase.getSprintById(sprintId);

                BaseResponse<SprintResponse> response = new BaseResponse<>(
                                "success",
                                "Sprint retrieved successfully",
                                sprint);

                return ResponseEntity.ok(response);
        }
        @GetMapping("/{sprintId}/complete_sprint/info")
        @Operation(summary = "Get a complete sprint info by sprint ID")
        public ResponseEntity<BaseResponse<CompleteSprintInfoResponse>> getCompleteSprintInfo(@PathVariable String sprintId) {
                CompleteSprintInfoResponse sprint = sprintUseCase.getCompleteSprintInfo(sprintId);

                BaseResponse<CompleteSprintInfoResponse> response = new BaseResponse<>(
                                "success",
                                "Sprint retrieved successfully",
                                sprint);

                return ResponseEntity.ok(response);
        }

        @PutMapping("/{sprintId}/start")
        @Operation(summary = "Start a sprint")
        public ResponseEntity<BaseResponse<SprintResponse>> startSprint(@PathVariable String sprintId) {
                SprintResponse startedSprint = sprintUseCase.startSprint(sprintId);

                BaseResponse<SprintResponse> response = new BaseResponse<>(
                                "success",
                                "Sprint started successfully",
                                startedSprint);

                return ResponseEntity.ok(response);
        }

        @PutMapping("/{sprintId}/complete")
        @Operation(summary = "Complete a sprint")
        public ResponseEntity<BaseResponse<SprintResponse>> complete(@PathVariable String sprintId) {
                SprintResponse startedSprint = sprintUseCase.completeSprint(sprintId);

                List<ProductBacklog> openBacklogs = productBacklogUseCase
                                .finProductBacklogBySprintAndStatusNot(sprintId, ProductBacklogStatus.DONE);

                for (ProductBacklog backlog : openBacklogs) {
                        productBacklogUseCase.reorderProductBacklog(
                                        backlog.getId(),
                                        sprintId,
                                        null,
                                        0);
                }

                BaseResponse<SprintResponse> response = new BaseResponse<>(
                                "success",
                                "Sprint completed successfully",
                                startedSprint);

                return ResponseEntity.ok(response);
        }

}
