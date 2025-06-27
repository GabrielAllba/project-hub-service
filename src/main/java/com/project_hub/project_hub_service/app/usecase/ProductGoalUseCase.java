package com.project_hub.project_hub_service.app.usecase;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.project_hub.project_hub_service.app.dtos.req.CreateProductGoalRequest;
import com.project_hub.project_hub_service.app.dtos.req.RenameProductGoalRequest;
import com.project_hub.project_hub_service.app.dtos.res.GetProductGoalByProjectResponse;
import com.project_hub.project_hub_service.app.dtos.res.ProductGoalResponse;
import com.project_hub.project_hub_service.app.entity.ProductBacklog;
import com.project_hub.project_hub_service.app.entity.ProductGoal;
import com.project_hub.project_hub_service.app.entity.Project;
import com.project_hub.project_hub_service.app.repository.postgres.ProductBacklogRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProductGoalRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectProductOwnerRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectScrumMasterRepository;

@Service
public class ProductGoalUseCase {

        @Autowired
        private ProductGoalRepository productGoalRepository;

        @Autowired
        private ProjectRepository projectRepository;

        @Autowired
        private ProductBacklogRepository productBacklogRepository;

        @Autowired
        private ProjectProductOwnerRepository projectProductOwnerRepository;

        @Autowired
        private ProjectScrumMasterRepository projectScrumMasterRepository;

        public ProductGoalResponse createProductGoal(CreateProductGoalRequest request) {
                String requesterId = getCurrentUserId();

                Project project = projectRepository.findById(request.getProjectId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Project not found"));

                if (!isProductOwnerOrScrumMaster(project.getId(), requesterId)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                        "Only a product owner or scrum master can create product goal");
                }

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

        public Page<GetProductGoalByProjectResponse> getProductGoalsByProjectId(String projectId, Pageable pageable) {
                boolean exists = projectRepository.existsById(projectId);
                if (!exists) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
                }

                Page<ProductGoal> productGoals = productGoalRepository.findByProjectId(projectId, pageable);

                return productGoals.map(goal -> {
                        int todoTask = productBacklogRepository.countTodoByProductGoal(goal.getId());
                        int inProgressTask = productBacklogRepository.countInProgressByProductGoal(goal.getId());
                        int doneTask = productBacklogRepository.countDoneByProductGoal(goal.getId());

                        return GetProductGoalByProjectResponse.builder()
                                        .id(goal.getId())
                                        .projectId(goal.getProject().getId())
                                        .title(goal.getTitle())
                                        .createdAt(goal.getCreatedAt())
                                        .updatedAt(goal.getUpdatedAt())
                                        .todoTask(todoTask)
                                        .inProgressTask(inProgressTask)
                                        .doneTask(doneTask)
                                        .build();
                });
        }

        public ProductGoalResponse renameProductGoal(RenameProductGoalRequest request) {
                String requesterId = getCurrentUserId();

                ProductGoal productGoal = productGoalRepository.findById(request.getProductGoalId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Product goal not found"));

                if (!isProductOwnerOrScrumMaster(productGoal.getProject().getId(), requesterId)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                        "Only a product owner or scrum master can rename a product goal");
                }

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
                String requesterId = getCurrentUserId();

                ProductGoal productGoal = productGoalRepository.findById(productGoalId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Product goal not found"));

                if (!isProductOwnerOrScrumMaster(productGoal.getProject().getId(), requesterId)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                        "Only a product owner or scrum master can delete a product goal");
                }

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

        private boolean isProductOwnerOrScrumMaster(String projectId, String userId) {
                return projectProductOwnerRepository.existsByProjectIdAndUserId(projectId, userId) ||
                                projectScrumMasterRepository.existsByProjectIdAndUserId(projectId, userId);
        }

        private String getCurrentUserId() {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                return auth.getPrincipal().toString();
        }
}
