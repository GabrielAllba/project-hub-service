package com.project_hub.project_hub_service.app.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.project_hub.project_hub_service.app.constants.ProductBacklogPriority;
import com.project_hub.project_hub_service.app.constants.ProductBacklogStatus;
import com.project_hub.project_hub_service.app.dtos.req.CreateProductBacklogRequest;
import com.project_hub.project_hub_service.app.dtos.res.ProductBacklogResponse;
import com.project_hub.project_hub_service.app.entity.ProductBacklog;
import com.project_hub.project_hub_service.app.entity.Project;
import com.project_hub.project_hub_service.app.repository.postgres.ProductBacklogRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectDeveloperRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectRepository;

@Service
public class ProductBacklogUseCase {

        private final ProductBacklogRepository productBacklogRepository;
        private final ProjectRepository projectRepository;
        private final ProjectDeveloperRepository projectDeveloperRepository;

        public ProductBacklogUseCase(ProductBacklogRepository productBacklogRepository,
                        ProjectRepository projectRepository, ProjectDeveloperRepository projectDeveloperRepository) {
                this.productBacklogRepository = productBacklogRepository;
                this.projectRepository = projectRepository;
                this.projectDeveloperRepository = projectDeveloperRepository;
        }

        public ProductBacklog create(String projectId, CreateProductBacklogRequest request) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String requesterId = authentication.getPrincipal().toString();

                boolean isProductOwner = projectRepository.existsByIdAndProductOwnerId(projectId, requesterId);
                boolean isScrumMaster = projectRepository.existsByIdAndScrumMasterId(projectId, requesterId);
                boolean isDeveloper = projectDeveloperRepository.existsByProjectIdAndUserId(projectId,
                                requesterId);

                if (!(isProductOwner || isScrumMaster || isDeveloper)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                        "You are not authorized to create a backlog for this project");
                }

                Project project = projectRepository.findById(projectId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Project not found"));

                ProductBacklog productBacklog = ProductBacklog.builder()
                                .title(request.getTitle())
                                .project(project)
                                .status(ProductBacklogStatus.TODO)
                                .priority(ProductBacklogPriority.LOW)
                                .creatorId(requesterId)
                                .build();

                return productBacklogRepository.save(productBacklog);
        }

        public Page<ProductBacklogResponse> getPaginatedBacklogsByProjectId(String projectId, Pageable pageable) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String requesterId = authentication.getPrincipal().toString();

                boolean isProductOwner = projectRepository.existsByIdAndProductOwnerId(projectId, requesterId);
                boolean isScrumMaster = projectRepository.existsByIdAndScrumMasterId(projectId, requesterId);
                boolean isDeveloper = projectDeveloperRepository.existsByProjectIdAndUserId(projectId,
                                requesterId);

                if (!(isProductOwner || isScrumMaster || isDeveloper)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                        "You are not authorized to get a backlog for this project");
                }

                boolean projectExists = projectRepository.existsById(projectId);
                if (!projectExists) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
                }

                return productBacklogRepository.findAllByProjectId(projectId, pageable)
                                .map(backlog -> ProductBacklogResponse.builder()
                                                .id(backlog.getId())
                                                .title(backlog.getTitle())
                                                .projectId(projectId)
                                                .priority(backlog.getPriority())
                                                .status(backlog.getStatus())
                                                .createdAt(backlog.getCreatedAt())
                                                .updatedAt(backlog.getUpdatedAt())
                                                .build());
        }

}
