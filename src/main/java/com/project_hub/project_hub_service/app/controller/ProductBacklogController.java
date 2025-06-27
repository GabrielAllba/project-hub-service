package com.project_hub.project_hub_service.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project_hub.project_hub_service.app.dtos.req.AssignBacklogUserRequest;
import com.project_hub.project_hub_service.app.dtos.req.CreateProductBacklogRequest;
import com.project_hub.project_hub_service.app.dtos.req.EditBacklogGoalRequest;
import com.project_hub.project_hub_service.app.dtos.req.EditBacklogPointRequest;
import com.project_hub.project_hub_service.app.dtos.req.EditBacklogPriorityRequest;
import com.project_hub.project_hub_service.app.dtos.req.EditBacklogStatusRequest;
import com.project_hub.project_hub_service.app.dtos.req.EditBacklogTitleRequest;
import com.project_hub.project_hub_service.app.dtos.req.ReorderProductBacklogRequest;
import com.project_hub.project_hub_service.app.dtos.res.GetMyActiveProductBacklogResponse;
import com.project_hub.project_hub_service.app.dtos.res.ProductBacklogResponse;
import com.project_hub.project_hub_service.app.usecase.ProductBacklogUseCase;
import com.project_hub.project_hub_service.misc.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/product_backlog")
@Tag(name = "Product Backlogs", description = "Manage product backlogs")
@SecurityRequirement(name = "bearerAuth")
public class ProductBacklogController {

        private final ProductBacklogUseCase productBacklogUseCase;

        @Autowired
        public ProductBacklogController(ProductBacklogUseCase productBacklogUseCase) {
                this.productBacklogUseCase = productBacklogUseCase;
        }

        @PostMapping("/{projectId}")
        @Operation(summary = "Create new product backlog in a project")
        public ResponseEntity<BaseResponse<ProductBacklogResponse>> createProductBacklog(
                        @Validated @RequestBody CreateProductBacklogRequest dto,
                        @PathVariable String projectId) {
                ProductBacklogResponse productBacklog = productBacklogUseCase.create(projectId, dto);

                BaseResponse<ProductBacklogResponse> response = new BaseResponse<>(
                                "success",
                                "Product backlog created successfully",
                                productBacklog);

                return ResponseEntity.ok(response);
        }

        @DeleteMapping("/{backlogId}")
        public ResponseEntity<BaseResponse<Void>> deleteBacklog(
                        @PathVariable String backlogId) {

                productBacklogUseCase.deleteBacklog(backlogId);

                BaseResponse<Void> response = new BaseResponse<>("success", "Backlog reordered successfully", null);
                return ResponseEntity.ok(response);
        }

        @PutMapping("/reorder")
        @Operation(summary = "Reorder product backlog based on drag and drop state")
        public ResponseEntity<BaseResponse<Void>> reorderProductBacklog(
                        @RequestBody @Validated ReorderProductBacklogRequest request) {

                productBacklogUseCase.reorderProductBacklog(
                                request.getActiveId(),
                                request.getOriginalContainer(),
                                request.getCurrentContainer(),
                                request.getInsertPosition());

                BaseResponse<Void> response = new BaseResponse<>("success", "Backlog reordered successfully", null);
                return ResponseEntity.ok(response);
        }

        @PutMapping("/edit_backlog_point")
        public ResponseEntity<BaseResponse<ProductBacklogResponse>> editBacklogPoint(
                        @Validated @RequestBody EditBacklogPointRequest dto) {

                ProductBacklogResponse updatedBacklog = productBacklogUseCase.editBacklogPoint(dto);

                BaseResponse<ProductBacklogResponse> response = new BaseResponse<>(
                                "success",
                                "Backlog point updated successfully",
                                updatedBacklog);

                return ResponseEntity.ok(response);
        }

        @GetMapping("/{backlogId}")
        @Operation(summary = "Get a backlog by ID")
        public ResponseEntity<BaseResponse<ProductBacklogResponse>> getBacklogById(@PathVariable String backlogId) {
                ProductBacklogResponse backlog = productBacklogUseCase.getBacklogById(backlogId);

                BaseResponse<ProductBacklogResponse> response = new BaseResponse<>(
                                "success",
                                "Backlog retrieved successfully",
                                backlog);

                return ResponseEntity.ok(response);
        }

        @PutMapping("/edit_backlog_priority")
        @Operation(summary = "Edit the priority of a backlog item")
        public ResponseEntity<BaseResponse<ProductBacklogResponse>> editBacklogPriority(
                        @Validated @RequestBody EditBacklogPriorityRequest dto) {

                ProductBacklogResponse updatedBacklog = productBacklogUseCase.editBacklogPriority(dto);

                BaseResponse<ProductBacklogResponse> response = new BaseResponse<>(
                                "success",
                                "Backlog priority updated successfully",
                                updatedBacklog);

                return ResponseEntity.ok(response);
        }

        @GetMapping("/my/active_sprint")
        @Operation(summary = "Get paginated product backlogs in IN_PROGRESS sprints assigned to the current user")
        public ResponseEntity<BaseResponse<Page<GetMyActiveProductBacklogResponse>>> getMyBacklogsFromActiveSprints(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
                Page<GetMyActiveProductBacklogResponse> backlogs = productBacklogUseCase.getMyBacklogsFromActiveSprints(pageable);

                BaseResponse<Page<GetMyActiveProductBacklogResponse>> response = new BaseResponse<>(
                                "success",
                                "Your active sprint backlogs retrieved successfully",
                                backlogs);

                return ResponseEntity.ok(response);
        }

        @PutMapping("/edit_backlog_status")
        @Operation(summary = "Edit the status of a backlog item")
        public ResponseEntity<BaseResponse<ProductBacklogResponse>> editBacklogPriority(
                        @Validated @RequestBody EditBacklogStatusRequest dto) {

                ProductBacklogResponse updatedBacklog = productBacklogUseCase.editBacklogStatus(dto);

                BaseResponse<ProductBacklogResponse> response = new BaseResponse<>(
                                "success",
                                "Backlog status updated successfully",
                                updatedBacklog);

                return ResponseEntity.ok(response);
        }

        @PutMapping("/edit_backlog_title")
        @Operation(summary = "Edit the title of a backlog item")
        public ResponseEntity<BaseResponse<ProductBacklogResponse>> editBacklogTitle(
                        @Validated @RequestBody EditBacklogTitleRequest dto) {

                ProductBacklogResponse updatedBacklog = productBacklogUseCase.editBacklogTitle(dto);

                BaseResponse<ProductBacklogResponse> response = new BaseResponse<>(
                                "success",
                                "Backlog title updated successfully",
                                updatedBacklog);

                return ResponseEntity.ok(response);
        }

        @PutMapping("/edit_backlog_goal")
        @Operation(summary = "Edit the goal of a backlog item")
        public ResponseEntity<BaseResponse<ProductBacklogResponse>> editBacklogGoal(
                        @Validated @RequestBody EditBacklogGoalRequest dto) {

                ProductBacklogResponse updatedBacklog = productBacklogUseCase.editBacklogGoal(dto);

                BaseResponse<ProductBacklogResponse> response = new BaseResponse<>(
                                "success",
                                "Backlog goal updated successfully",
                                updatedBacklog);

                return ResponseEntity.ok(response);
        }

        @PutMapping("/assign_user")
        @Operation(summary = "Assign a user to a backlog item")
        public ResponseEntity<BaseResponse<ProductBacklogResponse>> assignUserToBacklog(
                        @Validated @RequestBody AssignBacklogUserRequest dto) {

                ProductBacklogResponse updatedBacklog = productBacklogUseCase.assignUserToBacklog(dto);

                BaseResponse<ProductBacklogResponse> response = new BaseResponse<>(
                                "success",
                                "User assigned to backlog successfully",
                                updatedBacklog);

                return ResponseEntity.ok(response);
        }

}
