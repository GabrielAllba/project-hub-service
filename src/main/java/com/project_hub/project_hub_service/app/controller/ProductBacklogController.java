package com.project_hub.project_hub_service.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project_hub.project_hub_service.app.usecase.ProductBacklogUseCase;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/product_backlog")
@Tag(name = "Product Backlogs", description = "Manage product backlogs")
@SecurityRequirement(name = "bearerAuth")
public class ProductBacklogController {

    @Autowired
    private final ProductBacklogUseCase productBacklogUseCase;

    public ProductBacklogController(ProductBacklogUseCase productBacklogUseCase) {
        this.productBacklogUseCase = productBacklogUseCase;
    }

}
