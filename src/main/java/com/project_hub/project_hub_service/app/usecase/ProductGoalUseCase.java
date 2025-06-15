package com.project_hub.project_hub_service.app.usecase;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.project_hub.project_hub_service.app.dtos.req.CreateProductGoalRequest;
import com.project_hub.project_hub_service.app.dtos.req.RenameProductGoalRequest;
import com.project_hub.project_hub_service.app.dtos.res.ProductGoalResponse;
import com.project_hub.project_hub_service.app.entity.ProductBacklog;
import com.project_hub.project_hub_service.app.entity.ProductGoal;
import com.project_hub.project_hub_service.app.entity.Project;
import com.project_hub.project_hub_service.app.repository.postgres.ProductBacklogRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProductGoalRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectRepository;

@Service
public class ProductGoalUseCase {

        @Autowired
        private ProductGoalRepository productGoalRepository;

        @Autowired
        private ProjectRepository projectRepository;

        @Autowired
        private ProductBacklogRepository productBacklogRepository;

        public ProductGoalResponse createProductGoal(CreateProductGoalRequest request) {
                Project project = projectRepository.findById(request.getProjectId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Project not found"));

                ProductGoal productGoal = ProductGoal.builder()
                                .project(project)
                                .title(request.getTitle())
                                .build();

                ProductGoal savedGoal = productGoalRepository.save(productGoal);

                return ProductGoalResponse.builder()
                                .id(savedGoal.getId())
                                .projectId(savedGoal.getProject().getId())
                                .title(savedGoal.getTitle())
                                .createdAt(savedGoal.getCreatedAt())
                                .updatedAt(savedGoal.getUpdatedAt())
                                .build();
        }

        public Page<ProductGoalResponse> getProductGoalsByProjectId(String projectId,
                        Pageable pageable) {
                boolean exists = projectRepository.existsById(projectId);
                if (!exists) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
                }

                Page<ProductGoal> productGoals = productGoalRepository.findByProjectId(projectId, pageable);
                return productGoals.map(goal -> ProductGoalResponse.builder()
                                .id(goal.getId())
                                .projectId(goal.getProject().getId())
                                .title(goal.getTitle())
                                .createdAt(goal.getCreatedAt())
                                .updatedAt(goal.getUpdatedAt())
                                .build());
        }

        public ProductGoalResponse renameProductGoal(RenameProductGoalRequest request) {
                ProductGoal productGoal = productGoalRepository.findById(request.getProductGoalId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Product goal not found"));

                productGoal.setTitle(request.getNewTitle());

                ProductGoal updatedGoal = productGoalRepository.save(productGoal);

                return ProductGoalResponse.builder()
                                .id(updatedGoal.getId())
                                .projectId(updatedGoal.getProject().getId())
                                .title(updatedGoal.getTitle())
                                .createdAt(updatedGoal.getCreatedAt())
                                .updatedAt(updatedGoal.getUpdatedAt())
                                .build();
        }

        @Transactional
        public void deleteProductGoal(String productGoalId) {
                ProductGoal productGoal = productGoalRepository.findById(productGoalId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Product goal not found"));

                // Set related productBacklogs' productGoal to null
                List<ProductBacklog> relatedBacklogs = productBacklogRepository.findByProductGoalId(productGoalId);
                for (ProductBacklog backlog : relatedBacklogs) {
                        backlog.setProductGoal(null);
                }
                productBacklogRepository.saveAll(relatedBacklogs);

                // Delete the product goal
                productGoalRepository.delete(productGoal);
        }

        public ProductGoalResponse getProductGoalById(String productGoalId) {
                ProductGoal productGoal = productGoalRepository.findById(productGoalId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Product Goal not found"));

                return ProductGoalResponse.builder()
                                .id(productGoal.getId())
                                .projectId(productGoal.getProject().getId())
                                .title(productGoal.getTitle())
                                .createdAt(productGoal.getCreatedAt())
                                .updatedAt(productGoal.getUpdatedAt())
                                .build();
        }
}
