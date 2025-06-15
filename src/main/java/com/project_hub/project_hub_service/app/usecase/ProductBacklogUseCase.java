package com.project_hub.project_hub_service.app.usecase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.project_hub.project_hub_service.app.constants.ProductBacklogPriority;
import com.project_hub.project_hub_service.app.constants.ProductBacklogStatus;
import com.project_hub.project_hub_service.app.dtos.req.CreateProductBacklogRequest;
import com.project_hub.project_hub_service.app.dtos.req.EditBacklogGoalRequest;
import com.project_hub.project_hub_service.app.dtos.req.EditBacklogPointRequest;
import com.project_hub.project_hub_service.app.dtos.req.EditBacklogPriorityRequest;
import com.project_hub.project_hub_service.app.dtos.req.EditBacklogStatusRequest;
import com.project_hub.project_hub_service.app.dtos.req.EditBacklogTitleRequest;
import com.project_hub.project_hub_service.app.dtos.res.ProductBacklogResponse;
import com.project_hub.project_hub_service.app.entity.ProductBacklog;
import com.project_hub.project_hub_service.app.entity.ProductGoal;
import com.project_hub.project_hub_service.app.entity.Project;
import com.project_hub.project_hub_service.app.entity.Sprint;
import com.project_hub.project_hub_service.app.repository.postgres.ProductBacklogRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProductGoalRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectDeveloperRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectProductOwnerRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectScrumMasterRepository;
import com.project_hub.project_hub_service.app.repository.postgres.SprintRepository;

import jakarta.annotation.Nullable;

@Service
public class ProductBacklogUseCase {

        @Autowired
        private ProductBacklogRepository productBacklogRepository;

        @Autowired
        private ProjectRepository projectRepository;

        @Autowired
        private ProjectDeveloperRepository projectDeveloperRepository;

        @Autowired
        private SprintRepository sprintRepository;

        @Autowired
        private ProductGoalRepository productGoalRepository;

        @Autowired
        private ProjectProductOwnerRepository projectProductOwnerRepository;

        @Autowired
        private ProjectScrumMasterRepository projectScrumMasterRepository;

        public ProductBacklogResponse create(String projectId, CreateProductBacklogRequest request) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String requesterId = authentication.getPrincipal().toString();

                boolean isProductOwner = projectProductOwnerRepository.existsByProjectIdAndUserId(projectId,
                                requesterId);
                boolean isScrumMaster = projectScrumMasterRepository.existsByProjectIdAndUserId(projectId, requesterId);
                boolean isDeveloper = projectDeveloperRepository.existsByProjectIdAndUserId(projectId, requesterId);

                if (!(isProductOwner || isScrumMaster || isDeveloper)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                        "You are not authorized to create a backlog for this project");
                }

                Project project = projectRepository.findById(projectId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Project not found"));

                Sprint sprint = null;
                ProductBacklog lastBacklog;

                if (request.getSprintId() != null) {
                        sprint = sprintRepository.findById(request.getSprintId())
                                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                        "Sprint not found"));
                        lastBacklog = productBacklogRepository.findLastBacklogByProjectIdAndSprintId(projectId,
                                        sprint.getId());
                } else {
                        lastBacklog = productBacklogRepository.findLastBacklogByProjectIdAndSprintIsNull(projectId);
                }

                ProductBacklog productBacklog = ProductBacklog.builder()
                                .title(request.getTitle())
                                .project(project)
                                .sprint(sprint)
                                .status(ProductBacklogStatus.TODO)
                                .priority(ProductBacklogPriority.LOW)
                                .creatorId(requesterId)
                                .assigneeId(requesterId)
                                .prevBacklog(lastBacklog)
                                .build();

                ProductBacklog saved = productBacklogRepository.save(productBacklog);

                return ProductBacklogResponse.builder()
                                .id(saved.getId())
                                .title(saved.getTitle())
                                .projectId(projectId)
                                .sprintId(sprint != null ? sprint.getId() : null)
                                .priority(saved.getPriority())
                                .status(saved.getStatus())
                                .creatorId(saved.getCreatorId())
                                .assigneeId(saved.getAssigneeId())
                                .createdAt(saved.getCreatedAt())
                                .productGoalId(null)
                                .updatedAt(saved.getUpdatedAt())
                                .prevBacklogId(lastBacklog != null ? lastBacklog.getId() : null)
                                .point(saved.getPoint())
                                .build();
        }

        public Page<ProductBacklogResponse> getPaginatedBacklogsByProjectId(String projectId, Pageable pageable) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String requesterId = authentication.getPrincipal().toString();

                boolean isProductOwner = projectProductOwnerRepository.existsByProjectIdAndUserId(projectId,
                                requesterId);
                boolean isScrumMaster = projectScrumMasterRepository.existsByProjectIdAndUserId(projectId, requesterId);
                boolean isDeveloper = projectDeveloperRepository.existsByProjectIdAndUserId(projectId, requesterId);

                if (!(isProductOwner || isScrumMaster || isDeveloper)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                        "You are not authorized to get a backlog for this project");
                }

                Optional<Project> projectExists = projectRepository.findById(projectId);
                if (!projectExists.isPresent()) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
                }

                List<ProductBacklog> orderedBacklogs = getBacklogsOrderedByPrevBacklog(projectId, null);

                int start = (int) pageable.getOffset();
                int end = Math.min(start + pageable.getPageSize(), orderedBacklogs.size());

                if (start > end) {
                        return new PageImpl<>(Collections.emptyList(), pageable, orderedBacklogs.size());
                }

                List<ProductBacklogResponse> paged = orderedBacklogs.subList(start, end).stream()
                                .map(backlog -> ProductBacklogResponse.builder()
                                                .id(backlog.getId())
                                                .title(backlog.getTitle())
                                                .projectId(projectId)
                                                .sprintId(null)
                                                .priority(backlog.getPriority())
                                                .status(backlog.getStatus())
                                                .creatorId(backlog.getCreatorId())
                                                .assigneeId(backlog.getAssigneeId())
                                                .createdAt(backlog.getCreatedAt())
                                                .updatedAt(backlog.getUpdatedAt())
                                                .prevBacklogId(backlog.getPrevBacklog() != null
                                                                ? backlog.getPrevBacklog().getId()
                                                                : null)
                                                .productGoalId(backlog.getProductGoal() != null
                                                                ? backlog.getProductGoal().getId()
                                                                : null)
                                                .point(backlog.getPoint())
                                                .build())
                                .collect(Collectors.toList());

                return new PageImpl<>(paged, pageable, orderedBacklogs.size());
        }

        public Page<ProductBacklogResponse> getPaginatedBacklogsBySprintId(String sprintId, Pageable pageable) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String requesterId = authentication.getPrincipal().toString();

                Sprint sprint = sprintRepository.findById(sprintId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Sprint not found"));

                String projectId = sprint.getProject().getId();

                boolean isProductOwner = projectProductOwnerRepository.existsByProjectIdAndUserId(projectId,
                                requesterId);
                boolean isScrumMaster = projectScrumMasterRepository.existsByProjectIdAndUserId(projectId, requesterId);
                boolean isDeveloper = projectDeveloperRepository.existsByProjectIdAndUserId(projectId, requesterId);

                if (!(isProductOwner || isScrumMaster || isDeveloper)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                        "You are not authorized to view backlogs for this sprint");
                }

                List<ProductBacklog> allBacklogs = productBacklogRepository.findAllBySprintId(sprintId);
                List<ProductBacklog> orderedBacklogs = orderBacklogsByPrevBacklog(allBacklogs);

                int start = (int) pageable.getOffset();
                int end = Math.min(start + pageable.getPageSize(), orderedBacklogs.size());

                if (start > end) {
                        return new PageImpl<>(Collections.emptyList(), pageable, orderedBacklogs.size());
                }

                List<ProductBacklogResponse> paged = orderedBacklogs.subList(start, end).stream()
                                .map(backlog -> ProductBacklogResponse.builder()
                                                .id(backlog.getId())
                                                .title(backlog.getTitle())
                                                .projectId(projectId)
                                                .sprintId(backlog.getSprint() != null ? backlog.getSprint().getId()
                                                                : null)
                                                .priority(backlog.getPriority())
                                                .status(backlog.getStatus())
                                                .creatorId(backlog.getCreatorId())
                                                .assigneeId(backlog.getAssigneeId())
                                                .productGoalId(backlog.getProductGoal() != null
                                                                ? backlog.getProductGoal().getId()
                                                                : null)
                                                .createdAt(backlog.getCreatedAt())
                                                .updatedAt(backlog.getUpdatedAt())
                                                .point(backlog.getPoint())
                                                .build())
                                .collect(Collectors.toList());

                return new PageImpl<>(paged, pageable, orderedBacklogs.size());
        }

        private List<ProductBacklog> orderBacklogsByPrevBacklog(List<ProductBacklog> backlogs) {
                Map<String, ProductBacklog> prevBacklogMap = new HashMap<>();
                ProductBacklog head = null;

                for (ProductBacklog backlog : backlogs) {
                        String prevId = backlog.getPrevBacklog() != null ? backlog.getPrevBacklog().getId() : null;
                        prevBacklogMap.put(prevId, backlog);

                        if (prevId == null) {
                                head = backlog;
                        }
                }

                if (head == null) {
                        return Collections.emptyList();
                }

                List<ProductBacklog> ordered = new ArrayList<>();
                ProductBacklog current = head;

                while (current != null) {
                        ordered.add(current);
                        current = prevBacklogMap.get(current.getId());
                }
                return ordered;
        }

        private List<ProductBacklog> getBacklogsOrderedByPrevBacklog(String projectId, @Nullable String sprintId) {
                List<ProductBacklog> allBacklogs = productBacklogRepository.findAllByProjectIdAndSprintId(projectId,
                                sprintId);

                Map<String, ProductBacklog> prevBacklogMap = new HashMap<>();
                ProductBacklog head = null;

                for (ProductBacklog backlog : allBacklogs) {
                        String prevId = backlog.getPrevBacklog() != null ? backlog.getPrevBacklog().getId() : null;
                        prevBacklogMap.put(prevId, backlog);

                        if (prevId == null) {
                                head = backlog;
                        }
                }

                if (head == null) {
                        return Collections.emptyList();
                }

                List<ProductBacklog> ordered = new ArrayList<>();

                ProductBacklog current = head;
                while (current != null) {
                        ordered.add(current);
                        current = prevBacklogMap.get(current.getId());
                }

                return ordered;
        }

        @Transactional
        public void deleteBacklog(String backlogId) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String requesterId = authentication.getPrincipal().toString();

                ProductBacklog backlog = productBacklogRepository.findById(backlogId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Backlog not found"));

                // Validasi otorisasi user
                String projectId = backlog.getProject().getId();
                boolean isProductOwner = projectProductOwnerRepository.existsByProjectIdAndUserId(projectId,
                                requesterId);
                boolean isScrumMaster = projectScrumMasterRepository.existsByProjectIdAndUserId(projectId, requesterId);
                boolean isDeveloper = projectDeveloperRepository.existsByProjectIdAndUserId(projectId, requesterId);

                if (!(isProductOwner || isScrumMaster || isDeveloper)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                        "You are not authorized to delete this backlog");
                }
                // Update backlink (unlink dari prevBacklog)
                Optional<ProductBacklog> nextBacklogOpt = productBacklogRepository.findByPrevBacklog(backlog);
                if (nextBacklogOpt.isPresent()) {
                        ProductBacklog nextBacklog = nextBacklogOpt.get();
                        nextBacklog.setPrevBacklog(backlog.getPrevBacklog());
                        productBacklogRepository.save(nextBacklog);
                }

                productBacklogRepository.delete(backlog);
        }

        @Transactional
        public void reorderProductBacklog(String backlogId, @Nullable String sprintId, @Nullable String targetSprintId,
                        int insertPosition) {

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String requesterId = authentication.getPrincipal().toString();

                ProductBacklog backlogToMove = productBacklogRepository.findById(backlogId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Backlog not found"));

                String projectId = backlogToMove.getProject().getId();

                boolean isProductOwner = projectProductOwnerRepository.existsByProjectIdAndUserId(projectId,
                                requesterId);
                boolean isScrumMaster = projectScrumMasterRepository.existsByProjectIdAndUserId(projectId, requesterId);
                boolean isDeveloper = projectDeveloperRepository.existsByProjectIdAndUserId(projectId, requesterId);

                boolean isAuthorized = isProductOwner || isScrumMaster || isDeveloper;

                if (!isAuthorized) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                        "You are not authorized to reorder backlogs for this project");
                }

                Sprint targetSprint = null;
                if (targetSprintId != null) {
                        targetSprint = sprintRepository.findById(targetSprintId)
                                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                        "Target sprint not found"));

                        if (!targetSprint.getProject().getId().equals(projectId)) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "Target sprint does not belong to the same project");
                        }
                }

                List<ProductBacklog> targetList = getBacklogsOrderedByPrevBacklog(projectId, targetSprintId)
                                .stream()
                                .filter(item -> !item.getId().equals(backlogId)) // exclude the moving item
                                .collect(Collectors.toList());

                if (insertPosition < 0 || insertPosition > targetList.size()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Invalid insert position. Must be between 0 and " + targetList.size());
                }

                insertIntoLinkedList(backlogToMove, targetList, insertPosition, targetSprint);
        }

        private void insertIntoLinkedList(ProductBacklog backlogToMove, List<ProductBacklog> targetList,
                        int insertPosition, Sprint targetSprint) {

                String originalSprintId = backlogToMove.getSprint() != null ? backlogToMove.getSprint().getId() : null;
                String targetSprintId = targetSprint != null ? targetSprint.getId() : null;

                boolean isMovingAcrossSprint = originalSprintId == null
                                ? targetSprintId != null
                                : !originalSprintId.equals(targetSprintId);

                // 1. Remove backlog from its current position
                Optional<ProductBacklog> nextBacklogOpt = productBacklogRepository.findByPrevBacklog(backlogToMove);
                if (nextBacklogOpt.isPresent()) {
                        ProductBacklog nextBacklog = nextBacklogOpt.get();
                        nextBacklog.setPrevBacklog(backlogToMove.getPrevBacklog());
                        productBacklogRepository.save(nextBacklog);
                }

                // 2. Reset prevBacklog before re-inserting
                backlogToMove.setPrevBacklog(null);

                // 3. Update sprint if moving across container
                if (isMovingAcrossSprint) {
                        backlogToMove.setSprint(targetSprint);
                }

                // 4. Insert into new position
                if (targetList.isEmpty()) {
                        backlogToMove.setPrevBacklog(null);

                } else if (insertPosition == 0) {
                        ProductBacklog currentFirst = targetList.get(0);
                        backlogToMove.setPrevBacklog(null);
                        currentFirst.setPrevBacklog(backlogToMove);
                        productBacklogRepository.save(currentFirst);

                } else if (insertPosition == targetList.size()) {
                        ProductBacklog last = targetList.get(targetList.size() - 1);
                        backlogToMove.setPrevBacklog(last);

                } else {
                        ProductBacklog prev = targetList.get(insertPosition - 1);
                        ProductBacklog next = targetList.get(insertPosition);

                        backlogToMove.setPrevBacklog(prev);
                        next.setPrevBacklog(backlogToMove);
                        productBacklogRepository.save(next);
                }

                // 5. Save the moved backlog last
                productBacklogRepository.save(backlogToMove);
        }

        public ProductBacklogResponse getBacklogById(String backlogId) {
                ProductBacklog backlog = productBacklogRepository.findById(backlogId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Backlog not found"));

                return ProductBacklogResponse.builder()
                                .id(backlog.getId())
                                .title(backlog.getTitle())
                                .projectId(backlog.getProject().getId())
                                .sprintId(backlog.getSprint() != null ? backlog.getSprint().getId() : null)
                                .priority(backlog.getPriority())
                                .status(backlog.getStatus())
                                .creatorId(backlog.getCreatorId())
                                .assigneeId(backlog.getAssigneeId())
                                .createdAt(backlog.getCreatedAt())
                                .updatedAt(backlog.getUpdatedAt())
                                .prevBacklogId(backlog.getPrevBacklog() != null ? backlog.getPrevBacklog().getId()
                                                : null)
                                .productGoalId(backlog.getProductGoal() != null ? backlog.getProductGoal().getId()
                                                : null)
                                .point(backlog.getPoint())
                                .build();
        }

        public ProductBacklogResponse editBacklogPoint(EditBacklogPointRequest request) {
                ProductBacklog backlog = productBacklogRepository.findById(request.getBacklogId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Backlog not found"));

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String requesterId = authentication.getPrincipal().toString();

                boolean isProductOwner = projectProductOwnerRepository
                                .existsByProjectIdAndUserId(backlog.getProject().getId(), requesterId);
                boolean isScrumMaster = projectScrumMasterRepository
                                .existsByProjectIdAndUserId(backlog.getProject().getId(), requesterId);
                boolean isDeveloper = projectDeveloperRepository
                                .existsByProjectIdAndUserId(backlog.getProject().getId(), requesterId);

                if (!(isProductOwner || isScrumMaster || isDeveloper)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                        "You are not authorized to edit this backlog");
                }

                // Update point

                backlog.setPoint(request.getPoint());

                ProductBacklog saved = productBacklogRepository.save(backlog);

                return ProductBacklogResponse.builder()
                                .id(saved.getId())
                                .title(saved.getTitle())
                                .projectId(saved.getProject().getId())
                                .sprintId(saved.getSprint() != null ? saved.getSprint().getId() : null)
                                .priority(saved.getPriority())
                                .status(saved.getStatus())
                                .creatorId(saved.getCreatorId())
                                .assigneeId(saved.getAssigneeId())
                                .createdAt(saved.getCreatedAt())
                                .updatedAt(saved.getUpdatedAt())
                                .prevBacklogId(saved.getPrevBacklog() != null ? saved.getPrevBacklog().getId() : null)
                                .point(saved.getPoint())
                                .productGoalId(saved.getProductGoal() != null ? saved.getProductGoal().getId()
                                                : null)
                                .build();
        }

        public ProductBacklogResponse editBacklogPriority(EditBacklogPriorityRequest dto) {
                ProductBacklog backlog = productBacklogRepository.findById(dto.getBacklogId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Backlog not found"));

                backlog.setPriority(dto.getPriority());
                ProductBacklog saved = productBacklogRepository.save(backlog);

                return ProductBacklogResponse.builder()
                                .id(saved.getId())
                                .title(saved.getTitle())
                                .projectId(saved.getProject().getId())
                                .sprintId(saved.getSprint() != null ? saved.getSprint().getId() : null)
                                .priority(saved.getPriority())
                                .status(saved.getStatus())
                                .creatorId(saved.getCreatorId())
                                .assigneeId(saved.getAssigneeId())
                                .createdAt(saved.getCreatedAt())
                                .updatedAt(saved.getUpdatedAt())
                                .prevBacklogId(saved.getPrevBacklog() != null ? saved.getPrevBacklog().getId() : null)
                                .point(saved.getPoint())
                                .productGoalId(saved.getProductGoal() != null ? saved.getProductGoal().getId()
                                                : null)
                                .build();
        }

        public ProductBacklogResponse editBacklogStatus(EditBacklogStatusRequest dto) {
                ProductBacklog backlog = productBacklogRepository.findById(dto.getBacklogId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Backlog not found"));

                backlog.setStatus(dto.getStatus());
                ProductBacklog saved = productBacklogRepository.save(backlog);

                return ProductBacklogResponse.builder()
                                .id(saved.getId())
                                .title(saved.getTitle())
                                .projectId(saved.getProject().getId())
                                .sprintId(saved.getSprint() != null ? saved.getSprint().getId() : null)
                                .priority(saved.getPriority())
                                .status(saved.getStatus())
                                .creatorId(saved.getCreatorId())
                                .assigneeId(saved.getAssigneeId())
                                .createdAt(saved.getCreatedAt())
                                .updatedAt(saved.getUpdatedAt())
                                .prevBacklogId(saved.getPrevBacklog() != null ? saved.getPrevBacklog().getId() : null)
                                .point(saved.getPoint())
                                .productGoalId(saved.getProductGoal() != null ? saved.getProductGoal().getId()
                                                : null)
                                .build();
        }

        public ProductBacklogResponse editBacklogTitle(EditBacklogTitleRequest dto) {
                ProductBacklog backlog = productBacklogRepository.findById(dto.getBacklogId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Backlog not found"));

                backlog.setTitle(dto.getTitle());
                ProductBacklog saved = productBacklogRepository.save(backlog);

                return ProductBacklogResponse.builder()
                                .id(saved.getId())
                                .title(saved.getTitle())
                                .projectId(saved.getProject().getId())
                                .sprintId(saved.getSprint() != null ? saved.getSprint().getId() : null)
                                .priority(saved.getPriority())
                                .status(saved.getStatus())
                                .creatorId(saved.getCreatorId())
                                .assigneeId(saved.getAssigneeId())
                                .createdAt(saved.getCreatedAt())
                                .updatedAt(saved.getUpdatedAt())
                                .prevBacklogId(saved.getPrevBacklog() != null ? saved.getPrevBacklog().getId() : null)
                                .point(saved.getPoint())
                                .productGoalId(saved.getProductGoal() != null ? saved.getProductGoal().getId() : null)
                                .build();
        }

        public ProductBacklogResponse editBacklogGoal(EditBacklogGoalRequest dto) {
                ProductBacklog backlog = productBacklogRepository.findById(dto.getBacklogId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Backlog not found"));

                if (dto.getGoalId() != null) {
                        ProductGoal goal = productGoalRepository.findById(dto.getGoalId())
                                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                        "Product goal not found"));
                        backlog.setProductGoal(goal);
                } else {
                        backlog.setProductGoal(null);
                }

                ProductBacklog saved = productBacklogRepository.save(backlog);

                return ProductBacklogResponse.builder()
                                .id(saved.getId())
                                .title(saved.getTitle())
                                .projectId(saved.getProject().getId())
                                .sprintId(saved.getSprint() != null ? saved.getSprint().getId() : null)
                                .priority(saved.getPriority())
                                .status(saved.getStatus())
                                .creatorId(saved.getCreatorId())
                                .assigneeId(saved.getAssigneeId())
                                .createdAt(saved.getCreatedAt())
                                .updatedAt(saved.getUpdatedAt())
                                .prevBacklogId(saved.getPrevBacklog() != null ? saved.getPrevBacklog().getId() : null)
                                .point(saved.getPoint())
                                .productGoalId(saved.getProductGoal() != null ? saved.getProductGoal().getId()
                                                : null)
                                .build();
        }

        public List<ProductBacklog> finProductBacklogBySprintAndStatusNot(String sprintId,
                        ProductBacklogStatus status) {
                List<ProductBacklog> backlogs = productBacklogRepository.findBySprintIdAndStatusNot(
                                sprintId,
                                status);
                return backlogs;
        }
}