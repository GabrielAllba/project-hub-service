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

import com.project_hub.project_hub_service.app.dtos.req.CreateProductGoalRequest;
import com.project_hub.project_hub_service.app.dtos.req.RenameProductGoalRequest;
import com.project_hub.project_hub_service.app.dtos.res.ProductGoalResponse;
import com.project_hub.project_hub_service.app.dtos.res.SprintResponse;
import com.project_hub.project_hub_service.app.usecase.ProductGoalUseCase;
import com.project_hub.project_hub_service.misc.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Product Goals", description = "Endpoints for managing product goals")
@RestController
@RequestMapping("/api/product-goal")
@SecurityRequirement(name = "bearerAuth")
public class ProductGoalController {

    private final ProductGoalUseCase productGoalUseCase;

    @Autowired
    public ProductGoalController(ProductGoalUseCase productGoalUseCase) {
        this.productGoalUseCase = productGoalUseCase;
    }

    @PostMapping
    @Operation(summary = "Create a new product goal")
    public ResponseEntity<BaseResponse<ProductGoalResponse>> createProductGoal(
            @Validated @RequestBody CreateProductGoalRequest request) {

        ProductGoalResponse productGoal = productGoalUseCase.createProductGoal(request);

        BaseResponse<ProductGoalResponse> response = new BaseResponse<>(
                "success",
                "Product goal successfully created",
                productGoal);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/by_project/{projectId}")
    @Operation(summary = "Get product goals by project ID")
    public ResponseEntity<BaseResponse<Page<ProductGoalResponse>>> getProductGoalsByProjectId(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @PathVariable String projectId) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<ProductGoalResponse> productGoals = productGoalUseCase.getProductGoalsByProjectId(projectId, pageable);

        BaseResponse<Page<ProductGoalResponse>> response = new BaseResponse<>(
                "success",
                "Product goals retrieved successfully",
                productGoals);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/rename")
    @Operation(summary = "Rename a product goal")
    public ResponseEntity<BaseResponse<ProductGoalResponse>> renameProductGoal(
            @Validated @RequestBody RenameProductGoalRequest request) {

        ProductGoalResponse updatedGoal = productGoalUseCase.renameProductGoal(request);

        BaseResponse<ProductGoalResponse> response = new BaseResponse<>(
                "success",
                "Product goal renamed successfully",
                updatedGoal);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{productGoalId}")
    @Operation(summary = "Delete a product goal")
    public ResponseEntity<BaseResponse<Void>> deleteProductGoal(@PathVariable String productGoalId) {

        productGoalUseCase.deleteProductGoal(productGoalId);

        BaseResponse<Void> response = new BaseResponse<>(
                "success",
                "Product goal deleted successfully",
                null);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{productGoalId}")
    @Operation(summary = "Get a product goal by ID")
    public ResponseEntity<BaseResponse<ProductGoalResponse>> getSprintById(@PathVariable String productGoalId) {
        ProductGoalResponse productGoal = productGoalUseCase.getProductGoalById(productGoalId);

        BaseResponse<ProductGoalResponse> response = new BaseResponse<>(
                "success",
                "Product Goal retrieved successfully",
                productGoal);

        return ResponseEntity.ok(response);
    }
}
