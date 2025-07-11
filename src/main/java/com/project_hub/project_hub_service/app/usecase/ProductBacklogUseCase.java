package com.project_hub.project_hub_service.app.usecase;

import java.time.LocalDateTime;
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

import com.project_hub.project_hub_service.app.constants.BacklogActivityType;
import com.project_hub.project_hub_service.app.constants.ProductBacklogPriority;
import com.project_hub.project_hub_service.app.constants.ProductBacklogStatus;
import com.project_hub.project_hub_service.app.constants.SprintStatus;
import com.project_hub.project_hub_service.app.dtos.req.AssignBacklogUserRequest;
import com.project_hub.project_hub_service.app.dtos.req.CreateProductBacklogRequest;
import com.project_hub.project_hub_service.app.dtos.req.EditBacklogGoalRequest;
import com.project_hub.project_hub_service.app.dtos.req.EditBacklogPointRequest;
import com.project_hub.project_hub_service.app.dtos.req.EditBacklogPriorityRequest;
import com.project_hub.project_hub_service.app.dtos.req.EditBacklogStatusRequest;
import com.project_hub.project_hub_service.app.dtos.req.EditBacklogTitleRequest;
import com.project_hub.project_hub_service.app.dtos.res.GetMyActiveProductBacklogResponse;
import com.project_hub.project_hub_service.app.dtos.res.ProductBacklogResponse;
import com.project_hub.project_hub_service.app.dtos.res.ProjectBacklogSummaryResponse;
import com.project_hub.project_hub_service.app.dtos.res.UserWorkItemSummaryResponse;
import com.project_hub.project_hub_service.app.entity.BacklogActivityLog;
import com.project_hub.project_hub_service.app.entity.ProductBacklog;
import com.project_hub.project_hub_service.app.entity.ProductGoal;
import com.project_hub.project_hub_service.app.entity.Project;
import com.project_hub.project_hub_service.app.entity.Sprint;
import com.project_hub.project_hub_service.app.repository.gRpc.AuthenticationGrpcRepository;
import com.project_hub.project_hub_service.app.repository.postgres.BacklogActivityLogRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProductBacklogRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProductGoalRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectDeveloperRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectProductOwnerRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectScrumMasterRepository;
import com.project_hub.project_hub_service.app.repository.postgres.SprintRepository;

import authenticationservice.AuthenticationServiceOuterClass.FindUserResponse;
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

        @Autowired
        private AuthenticationGrpcRepository authenticationGrpcRepository;

        @Autowired
        private BacklogActivityLogRepository backlogActivityLogRepository;

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

                FindUserResponse user = findUserOrThrow(productBacklog.getAssigneeId());

                BacklogActivityLog activityLog = BacklogActivityLog.builder()
                                .backlog(productBacklog)
                                .userId(requesterId)
                                .activityType(BacklogActivityType.BACKLOG_CREATED)
                                .description("Backlog '" + saved.getTitle() + "' was created by " + user.getUsername()
                                                + ".")
                                .oldValue(null)
                                .newValue(saved.getTitle())
                                .build();

                backlogActivityLogRepository.save(activityLog);

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
                                .productGoalId(null) // Assuming productGoal is handled separately or can be null
                                                     // initially
                                .updatedAt(saved.getUpdatedAt())
                                .point(saved.getPoint())
                                .prevBacklogId(lastBacklog != null ? lastBacklog.getId() : null)
                                .build();
        }

        public Page<ProductBacklogResponse> getPaginatedBacklogsByProjectId(
                        String projectId,
                        String search,
                        ProductBacklogStatus status,
                        ProductBacklogPriority priority,
                        List<String> productGoalId,
                        List<String> assigneeIds,
                        Pageable pageable) {

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

                Project project = projectRepository.findById(projectId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Project not found"));

                // Get all backlogs in order
                List<ProductBacklog> orderedBacklogs = getBacklogsOrderedByPrevBacklog(projectId, null);

                boolean includeNoGoal = productGoalId != null && productGoalId.contains("no-goal");

                // Apply filtering
                List<ProductBacklog> filtered = orderedBacklogs.stream()
                                .filter(b -> search == null
                                                || b.getTitle().toLowerCase().contains(search.toLowerCase()))
                                .filter(b -> status == null || b.getStatus() == status)
                                .filter(b -> priority == null || b.getPriority() == priority)
                                .filter(b -> {
                                        if (productGoalId == null || productGoalId.isEmpty()) {
                                                return true;
                                        }
                                        if (b.getProductGoal() == null) {
                                                return includeNoGoal;
                                        }
                                        return productGoalId.contains(b.getProductGoal().getId());
                                })
                                .filter(b -> {
                                        if (assigneeIds == null || assigneeIds.isEmpty()) {
                                                return true;
                                        }
                                        return assigneeIds.contains(b.getAssigneeId());
                                })
                                .collect(Collectors.toList());

                // Apply pagination
                int start = (int) pageable.getOffset();
                int end = Math.min(start + pageable.getPageSize(), filtered.size());

                if (start >= filtered.size()) {
                        return new PageImpl<>(Collections.emptyList(), pageable, filtered.size());
                }

                List<ProductBacklogResponse> paged = filtered.subList(start, end).stream()
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

                return new PageImpl<>(paged, pageable, filtered.size());
        }

        public Page<ProductBacklogResponse> getPaginatedBacklogsBySprintId(
                        String sprintId,
                        String search,
                        ProductBacklogStatus status,
                        ProductBacklogPriority priority,
                        List<String> productGoalId,
                        List<String> assigneeIds,
                        Pageable pageable) {

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

                boolean includeNoGoal = productGoalId != null && productGoalId.contains("no-goal");

                // Apply filters
                List<ProductBacklog> filtered = orderedBacklogs.stream()
                                .filter(b -> search == null
                                                || b.getTitle().toLowerCase().contains(search.toLowerCase()))
                                .filter(b -> status == null || b.getStatus() == status)
                                .filter(b -> priority == null || b.getPriority() == priority)
                                .filter(b -> {
                                        if (productGoalId == null || productGoalId.isEmpty())
                                                return true;
                                        if (b.getProductGoal() == null)
                                                return includeNoGoal;
                                        return productGoalId.contains(b.getProductGoal().getId());
                                })
                                .filter(b -> {
                                        if (assigneeIds == null || assigneeIds.isEmpty())
                                                return true;
                                        return assigneeIds.contains(b.getAssigneeId());
                                })
                                .collect(Collectors.toList());

                int start = (int) pageable.getOffset();
                int end = Math.min(start + pageable.getPageSize(), filtered.size());

                if (start >= filtered.size()) {
                        return new PageImpl<>(Collections.emptyList(), pageable, filtered.size());
                }

                List<ProductBacklogResponse> paged = filtered.subList(start, end).stream()
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

                return new PageImpl<>(paged, pageable, filtered.size());
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

                // Hapus semua log aktivitas yang terkait backlog ini
                backlogActivityLogRepository.deleteAllByBacklog(backlog);

                // Hapus backlog-nya
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

                String oldSprintName = backlogToMove.getSprint() != null ? backlogToMove.getSprint().getName()
                                : "Backlog";
                String newSprintName = targetSprint != null ? targetSprint.getName() : "Backlog";
                insertIntoLinkedList(backlogToMove, targetList, insertPosition, targetSprint);

                // Logging activity after successful reorder
                FindUserResponse user = findUserOrThrow(requesterId);
                String description = String.format(
                                "Backlog '%s' was moved by %s from %s to %s at position %d.",
                                backlogToMove.getTitle(),
                                user.getUsername(),
                                oldSprintName,
                                newSprintName,
                                insertPosition + 1 // +1 to make it 1-based index in log
                );

                BacklogActivityLog activityLog = BacklogActivityLog.builder()
                                .backlog(backlogToMove)
                                .userId(requesterId)
                                .activityType(BacklogActivityType.BACKLOG_REORDERED)
                                .description(description)
                                .oldValue(oldSprintName)
                                .newValue(newSprintName)
                                .build();

                backlogActivityLogRepository.save(activityLog);
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

                validateBacklogEditPermission(backlog.getProject().getId());

                Integer oldPoint = backlog.getPoint();
                backlog.setPoint(request.getPoint());

                ProductBacklog saved = productBacklogRepository.save(backlog);

                backlogActivityLogRepository.save(
                                BacklogActivityLog.builder()
                                                .backlog(saved)
                                                .userId(getCurrentUserId())
                                                .activityType(BacklogActivityType.POINT_CHANGE)
                                                .oldValue(oldPoint != null ? String.valueOf(oldPoint) : null)
                                                .newValue(String.valueOf(request.getPoint()))
                                                .description("Point changed from " + oldPoint + " to "
                                                                + request.getPoint())
                                                .build());

                return toBacklogResponse(saved);
        }

        public ProductBacklogResponse editBacklogPriority(EditBacklogPriorityRequest request) {
                ProductBacklog backlog = productBacklogRepository.findById(request.getBacklogId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Backlog not found"));

                validateBacklogEditPermission(backlog.getProject().getId());

                ProductBacklogPriority oldPriority = backlog.getPriority();
                backlog.setPriority(request.getPriority());

                ProductBacklog saved = productBacklogRepository.save(backlog);

                backlogActivityLogRepository.save(
                                BacklogActivityLog.builder()
                                                .backlog(saved)
                                                .userId(getCurrentUserId())
                                                .activityType(BacklogActivityType.PRIORITY_CHANGE)
                                                .oldValue(oldPriority.name())
                                                .newValue(request.getPriority().name())
                                                .description("Priority changed from " + oldPriority.name() + " to "
                                                                + request.getPriority().name())
                                                .build());

                return toBacklogResponse(saved);
        }

        public ProductBacklogResponse editBacklogStatus(EditBacklogStatusRequest request) {
                ProductBacklog backlog = productBacklogRepository.findById(request.getBacklogId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Backlog not found"));

                validateBacklogEditPermission(backlog.getProject().getId());

                ProductBacklogStatus oldStatus = backlog.getStatus();
                backlog.setStatus(request.getStatus());

                ProductBacklog saved = productBacklogRepository.save(backlog);

                backlogActivityLogRepository.save(
                                BacklogActivityLog.builder()
                                                .backlog(saved)
                                                .userId(getCurrentUserId())
                                                .activityType(BacklogActivityType.STATUS_CHANGE)
                                                .oldValue(oldStatus.name())
                                                .newValue(request.getStatus().name())
                                                .description("Status changed from " + oldStatus.name() + " to "
                                                                + request.getStatus().name())
                                                .build());

                return toBacklogResponse(saved);
        }

        public ProductBacklogResponse editBacklogTitle(EditBacklogTitleRequest request) {
                ProductBacklog backlog = productBacklogRepository.findById(request.getBacklogId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Backlog not found"));

                validateBacklogEditPermission(backlog.getProject().getId());

                String oldTitle = backlog.getTitle();
                backlog.setTitle(request.getTitle());

                ProductBacklog saved = productBacklogRepository.save(backlog);

                backlogActivityLogRepository.save(
                                BacklogActivityLog.builder()
                                                .backlog(saved)
                                                .userId(getCurrentUserId())
                                                .activityType(BacklogActivityType.TITLE_CHANGE)
                                                .oldValue(oldTitle)
                                                .newValue(request.getTitle())
                                                .description("Title changed from '" + oldTitle + "' to '"
                                                                + request.getTitle() + "'")
                                                .build());

                return toBacklogResponse(saved);
        }

        public ProductBacklogResponse editBacklogGoal(EditBacklogGoalRequest request) {
                ProductBacklog backlog = productBacklogRepository.findById(request.getBacklogId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Backlog not found"));

                validateBacklogEditPermission(backlog.getProject().getId());

                String oldGoalTitle = backlog.getProductGoal() != null ? backlog.getProductGoal().getTitle() : null;

                if (request.getGoalId() != null) {
                        ProductGoal goal = productGoalRepository.findById(request.getGoalId())
                                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                        "Product goal not found"));
                        backlog.setProductGoal(goal);
                } else {
                        backlog.setProductGoal(null);
                }

                ProductBacklog saved = productBacklogRepository.save(backlog);
                String newGoalTitle = saved.getProductGoal() != null ? saved.getProductGoal().getTitle() : null;

                backlogActivityLogRepository.save(
                                BacklogActivityLog.builder()
                                                .backlog(saved)
                                                .userId(getCurrentUserId())
                                                .activityType(BacklogActivityType.GOAL_CHANGE)
                                                .oldValue(oldGoalTitle)
                                                .newValue(newGoalTitle)
                                                .description("Changed product goal from " +
                                                                (oldGoalTitle != null ? "'" + oldGoalTitle + "'"
                                                                                : "none")
                                                                +
                                                                " to " +
                                                                (newGoalTitle != null ? "'" + newGoalTitle + "'"
                                                                                : "none")
                                                                + ".")
                                                .build());

                return toBacklogResponse(saved);
        }

        public List<ProductBacklog> finProductBacklogBySprintAndStatusNot(String sprintId,
                        ProductBacklogStatus status) {
                List<ProductBacklog> backlogs = productBacklogRepository.findBySprintIdAndStatusNot(
                                sprintId,
                                status);
                return backlogs;
        }

        public ProductBacklogResponse assignUserToBacklog(AssignBacklogUserRequest dto) {
                ProductBacklog backlog = productBacklogRepository.findById(dto.getBacklogId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Backlog not found"));

                String projectId = backlog.getProject().getId();
                String assigneeId = dto.getAssigneeId();

                boolean isTeamMember = projectDeveloperRepository.existsByProjectIdAndUserId(projectId, assigneeId)
                                || projectScrumMasterRepository.existsByProjectIdAndUserId(projectId, assigneeId)
                                || projectProductOwnerRepository.existsByProjectIdAndUserId(projectId, assigneeId);

                if (!isTeamMember) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "User with id " + assigneeId + " is not a member of the project team");
                }

                String oldAssigneeId = backlog.getAssigneeId();
                backlog.setAssigneeId(assigneeId);
                ProductBacklog saved = productBacklogRepository.save(backlog);

                // Fetch usernames for old and new assignee
                String oldUsername = oldAssigneeId != null ? findUserOrThrow(oldAssigneeId).getUsername()
                                : "Unassigned";
                String newUsername = findUserOrThrow(assigneeId).getUsername();

                backlogActivityLogRepository.save(
                                BacklogActivityLog.builder()
                                                .backlog(saved)
                                                .userId(getCurrentUserId())
                                                .activityType(BacklogActivityType.ASSIGNEE_CHANGE)
                                                .oldValue(oldUsername)
                                                .newValue(newUsername)
                                                .description("Assignee changed from " + oldUsername + " to "
                                                                + newUsername)
                                                .build());

                return toBacklogResponse(saved);
        }

        public ProjectBacklogSummaryResponse getProjectBacklogSummary(String projectId) {
                List<Sprint> activeSprints = sprintRepository
                                .findAllByProjectIdAndStatus(projectId, SprintStatus.IN_PROGRESS);

                // Jika tidak ada active sprint, langsung return 0 semua
                if (activeSprints.isEmpty()) {
                        return ProjectBacklogSummaryResponse.builder()
                                        .totalTodo(0)
                                        .totalInProgress(0)
                                        .totalDone(0)
                                        .build();
                }

                List<String> sprintIds = activeSprints.stream()
                                .map(Sprint::getId)
                                .toList();

                List<ProductBacklog> backlogs = productBacklogRepository.findBySprintIdIn(sprintIds);

                int todo = 0, inProgress = 0, done = 0;

                for (ProductBacklog backlog : backlogs) {
                        switch (backlog.getStatus()) {
                                case TODO -> todo++;
                                case INPROGRESS -> inProgress++;
                                case DONE -> done++;
                        }
                }

                return ProjectBacklogSummaryResponse.builder()
                                .totalTodo(todo)
                                .totalInProgress(inProgress)
                                .totalDone(done)
                                .build();
        }

        public List<UserWorkItemSummaryResponse> getWorkSummaryByTeamAndDateRange(String projectId, String range) {
                // 1. Determine the start date based on the range
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime startDate = switch (range) {
                        case "30d" -> now.minusDays(30);
                        case "3m" -> now.minusMonths(3);
                        default -> now.minusDays(7); // fallback to 7 days
                };

                // 2. Get sprints in the project that intersect with the date range
                List<Sprint> sprints = sprintRepository.findByProjectIdAndDateRange(projectId, startDate, now);

                if (sprints.isEmpty()) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                                        "No sprints found in the selected date range: start date: " + startDate);
                }

                List<String> sprintIds = sprints.stream().map(Sprint::getId).toList();

                // 3. Get backlog items within those sprints
                List<ProductBacklog> backlogs = productBacklogRepository.findBySprintIdIn(sprintIds);

                // 4. Group and count by assignee
                Map<String, UserWorkItemSummaryResponse> summaryMap = new HashMap<>();

                for (ProductBacklog backlog : backlogs) {
                        if (backlog.getAssigneeId() == null)
                                continue;

                        String email;
                        try {
                                email = authenticationGrpcRepository.findUser(backlog.getAssigneeId()).getEmail();
                        } catch (Exception e) {
                                continue; // Skip if user info is unavailable
                        }

                        UserWorkItemSummaryResponse summary = summaryMap.getOrDefault(
                                        email,
                                        UserWorkItemSummaryResponse.builder()
                                                        .email(email)
                                                        .todo(0)
                                                        .inProgress(0)
                                                        .done(0)
                                                        .build());

                        switch (backlog.getStatus()) {
                                case TODO -> summary.setTodo(summary.getTodo() + 1);
                                case INPROGRESS -> summary.setInProgress(summary.getInProgress() + 1);
                                case DONE -> summary.setDone(summary.getDone() + 1);
                        }

                        summaryMap.put(email, summary);
                }

                return new ArrayList<>(summaryMap.values());
        }

        public Page<GetMyActiveProductBacklogResponse> getMyBacklogsFromActiveSprints(Pageable pageable) {
                String userId = getCurrentUserId();

                Page<ProductBacklog> backlogs = productBacklogRepository
                                .findBySprintStatusAndAssigneeId(SprintStatus.IN_PROGRESS, userId, pageable);

                return backlogs.map(backlog -> GetMyActiveProductBacklogResponse.builder()
                                .id(backlog.getId())
                                .prevBacklogId(backlog.getPrevBacklog() != null ? backlog.getPrevBacklog().getId()
                                                : null)
                                .projectId(backlog.getProject() != null ? backlog.getProject().getId() : null)
                                .projectName(backlog.getProject() != null ? backlog.getProject().getName() : null)
                                .sprintId(backlog.getSprint() != null ? backlog.getSprint().getId() : null)
                                .sprintName(backlog.getSprint() != null ? backlog.getSprint().getName() : null)
                                .productGoalId(backlog.getProductGoal() != null ? backlog.getProductGoal().getId()
                                                : null)
                                .productGoalTitle(backlog.getProductGoal() != null ? backlog.getProductGoal().getTitle()
                                                : null)
                                .point(backlog.getPoint())
                                .title(backlog.getTitle())
                                .priority(backlog.getPriority())
                                .status(backlog.getStatus())
                                .creatorId(backlog.getCreatorId())
                                .assigneeId(backlog.getAssigneeId())
                                .createdAt(backlog.getCreatedAt())
                                .updatedAt(backlog.getUpdatedAt())
                                .build());
        }

        private String getCurrentUserId() {
                return SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        }

        private FindUserResponse findUserOrThrow(String userId) {
                try {
                        return authenticationGrpcRepository.findUser(userId);
                } catch (Exception e) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                                        "User with id " + userId + " not found");
                }
        }

        private void validateBacklogEditPermission(String projectId) {
                String userId = getCurrentUserId();

                boolean isAuthorized = projectProductOwnerRepository.existsByProjectIdAndUserId(projectId, userId)
                                || projectScrumMasterRepository.existsByProjectIdAndUserId(projectId, userId)
                                || projectDeveloperRepository.existsByProjectIdAndUserId(projectId, userId);

                if (!isAuthorized) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                        "You are not authorized to edit this backlog");
                }
        }

        private ProductBacklogResponse toBacklogResponse(ProductBacklog backlog) {
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
                                .point(backlog.getPoint())
                                .productGoalId(backlog.getProductGoal() != null ? backlog.getProductGoal().getId()
                                                : null)
                                .build();
        }

}