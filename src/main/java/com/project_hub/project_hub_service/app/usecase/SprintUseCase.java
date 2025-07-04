package com.project_hub.project_hub_service.app.usecase;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.project_hub.project_hub_service.app.constants.ProductBacklogStatus;
import com.project_hub.project_hub_service.app.constants.SprintStatus;
import com.project_hub.project_hub_service.app.dtos.req.CreateSprintRequest;
import com.project_hub.project_hub_service.app.dtos.req.EditSprintGoalAndDatesRequest;
import com.project_hub.project_hub_service.app.dtos.res.CompleteSprintInfoResponse;
import com.project_hub.project_hub_service.app.dtos.res.SprintOverviewResponse;
import com.project_hub.project_hub_service.app.dtos.res.SprintResponse;
import com.project_hub.project_hub_service.app.dtos.res.TimelineSprintResponse;
import com.project_hub.project_hub_service.app.dtos.res.UserTaskDistributionResponse;
import com.project_hub.project_hub_service.app.entity.ProductBacklog;
import com.project_hub.project_hub_service.app.entity.Project;
import com.project_hub.project_hub_service.app.entity.Sprint;
import com.project_hub.project_hub_service.app.repository.gRpc.AuthenticationGrpcRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProductBacklogRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectProductOwnerRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectScrumMasterRepository;
import com.project_hub.project_hub_service.app.repository.postgres.SprintRepository;

import authenticationservice.AuthenticationServiceOuterClass.FindUserResponse;

@Service
public class SprintUseCase {

        @Autowired
        private SprintRepository sprintRepository;

        @Autowired
        private ProjectRepository projectRepository;

        @Autowired
        private ProductBacklogRepository productBacklogRepository;

        @Autowired
        private ProjectProductOwnerRepository projectProductOwnerRepository;

        @Autowired
        private ProjectScrumMasterRepository projectScrumMasterRepository;

        @Autowired
        private AuthenticationGrpcRepository authenticationGrpcRepository;

        public SprintResponse create(CreateSprintRequest request) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String requesterId = authentication.getPrincipal().toString();

                Project project = projectRepository.findById(request.getProjectId())
                                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

                if (!isUserAuthorized(project, requesterId)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                        "You are not authorized to create sprint, only the product owner & scrum master of this project can create sprint.");
                }

                // --- Logic to determine the next sprint number ---
                List<Sprint> existingSprints = sprintRepository.findByProjectId(project.getId());

                int nextSprintNumber = 1; // Default for the very first sprint
                if (!existingSprints.isEmpty()) {
                        // Find the maximum sprint number used so far
                        // We need to parse the "Sprint X" name to get the number
                        Optional<Integer> maxNumber = existingSprints.stream()
                                        .map(sprint -> {
                                                String name = sprint.getName();
                                                if (name != null && name.startsWith("Sprint ")) {
                                                        try {
                                                                return Integer.parseInt(
                                                                                name.substring("Sprint ".length()));
                                                        } catch (NumberFormatException e) {
                                                                // Handle cases where sprint name might not be in
                                                                // "Sprint X" format
                                                                // or log a warning. For simplicity, we'll ignore
                                                                // malformed names
                                                                // for max number calculation.
                                                                return 0; // Treat as 0 or some other indicator
                                                        }
                                                }
                                                return 0; // If name doesn't start with "Sprint "
                                        })
                                        .max(Comparator.naturalOrder()); // Find the maximum number

                        if (maxNumber.isPresent()) {
                                nextSprintNumber = maxNumber.get() + 1;
                        }
                }
                String sprintName = "Sprint " + nextSprintNumber;
                // --- End of logic to determine the next sprint number ---

                Sprint sprint = Sprint.builder()
                                .project(project)
                                .name(sprintName) // Set the determined sprint name
                                .sprintGoal("")
                                .status(SprintStatus.NOT_STARTED)
                                .build();

                Sprint saved = sprintRepository.save(sprint);

                DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

                return SprintResponse.builder()
                                .id(saved.getId().toString())
                                .projectId(saved.getProject().getId().toString())
                                .name(saved.getName())
                                .startDate(saved.getStartDate() != null
                                                ? saved.getStartDate().format(formatter)
                                                : null)
                                .endDate(saved.getEndDate() != null ? saved.getEndDate().format(formatter)
                                                : null)
                                .createdAt(saved.getCreatedAt().toString())
                                .updatedAt(saved.getUpdatedAt().toString())
                                .sprintGoal(saved.getSprintGoal())
                                .status(saved.getStatus().toString())
                                .build();
        }

        public SprintResponse getSprintById(String sprintId) {
                Sprint sprint = sprintRepository.findById(sprintId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Sprint not found"));

                DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

                return SprintResponse.builder()
                                .id(sprint.getId().toString())
                                .projectId(sprint.getProject().getId().toString())
                                .name(sprint.getName())
                                .startDate(sprint.getStartDate() != null ? sprint.getStartDate().format(formatter)
                                                : null)
                                .endDate(sprint.getEndDate() != null ? sprint.getEndDate().format(formatter) : null)
                                .createdAt(sprint.getCreatedAt().toString())
                                .updatedAt(sprint.getUpdatedAt().toString())
                                .sprintGoal(sprint.getSprintGoal())
                                .status(sprint.getStatus().toString())
                                .build();
        }

        public Page<SprintResponse> getPaginatedSprintsByProjectId(String projectId, Pageable pageable) {
                if (!projectRepository.existsById(projectId)) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
                }

                Page<Sprint> sprintPage = sprintRepository.findActiveSprintsByProjectOrdered(projectId, pageable);
                // Page<Sprint> sprintPage = sprintRepository.findByProjectId(projectId,
                // pageable);

                DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

                return sprintPage.map(sprint -> SprintResponse.builder()
                                .id(sprint.getId())
                                .projectId(sprint.getProject().getId())
                                .name(sprint.getName())
                                .startDate(sprint.getStartDate() != null ? sprint.getStartDate().format(formatter)
                                                : null)
                                .endDate(sprint.getEndDate() != null ? sprint.getEndDate().format(formatter) : null)
                                .createdAt(sprint.getCreatedAt().toString())
                                .updatedAt(sprint.getUpdatedAt().toString())
                                .sprintGoal(sprint.getSprintGoal())
                                .status(sprint.getStatus().toString())
                                .build());
        }

        public Page<SprintResponse> getPaginatedSprintsByProjectIdAllStatus(String projectId, Pageable pageable) {
                if (!projectRepository.existsById(projectId)) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
                }

                // Page<Sprint> sprintPage =
                // sprintRepository.findActiveSprintsByProjectOrdered(projectId, pageable);
                Page<Sprint> sprintPage = sprintRepository.findByProjectId(projectId,
                                pageable);

                DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

                return sprintPage.map(sprint -> SprintResponse.builder()
                                .id(sprint.getId())
                                .projectId(sprint.getProject().getId())
                                .name(sprint.getName())
                                .startDate(sprint.getStartDate() != null ? sprint.getStartDate().format(formatter)
                                                : null)
                                .endDate(sprint.getEndDate() != null ? sprint.getEndDate().format(formatter) : null)
                                .createdAt(sprint.getCreatedAt().toString())
                                .updatedAt(sprint.getUpdatedAt().toString())
                                .sprintGoal(sprint.getSprintGoal())
                                .status(sprint.getStatus().toString())
                                .build());
        }

        public Page<TimelineSprintResponse> getPaginatedSprintsTimelineByProjectIdAndYear(String projectId, int year,
                        Pageable pageable) {

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String requesterId = authentication.getPrincipal().toString();

                FindUserResponse user = findUserOrThrow(requesterId);

                if (!projectRepository.existsById(projectId)) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
                }

                LocalDateTime start = LocalDateTime.of(year, 1, 1, 0, 0);
                LocalDateTime end = LocalDateTime.of(year, 12, 31, 23, 59, 59);

                Page<Sprint> sprintPage = sprintRepository.findWithDateRangeAndPagination(projectId, start, end,
                                pageable);

                DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
                return sprintPage.map(sprint -> {
                        int taskCount = productBacklogRepository.countBySprintIdAndAssigneeId(sprint.getId(),
                                        user.getId());

                        return TimelineSprintResponse.builder()
                                        .id(sprint.getId())
                                        .projectId(sprint.getProject().getId())
                                        .name(sprint.getName())
                                        .startDate(sprint.getStartDate() != null
                                                        ? sprint.getStartDate().format(formatter)
                                                        : null)
                                        .endDate(sprint.getEndDate() != null ? sprint.getEndDate().format(formatter)
                                                        : null)
                                        .createdAt(sprint.getCreatedAt().toString())
                                        .updatedAt(sprint.getUpdatedAt().toString())
                                        .sprintGoal(sprint.getSprintGoal())
                                        .status(sprint.getStatus().toString())
                                        .userTask(taskCount)
                                        .build();
                });

        }

        public Page<SprintResponse> getPaginatedInProgressSprintsByProjectId(String projectId, Pageable pageable) {
                if (!projectRepository.existsById(projectId)) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
                }

                Page<Sprint> sprintPage = sprintRepository.findInProgressSprintsByProjectOrdered(projectId, pageable);

                DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

                return sprintPage.map(sprint -> SprintResponse.builder()
                                .id(sprint.getId())
                                .projectId(sprint.getProject().getId())
                                .name(sprint.getName())
                                .startDate(sprint.getStartDate() != null ? sprint.getStartDate().format(formatter)
                                                : null)
                                .endDate(sprint.getEndDate() != null ? sprint.getEndDate().format(formatter) : null)
                                .createdAt(sprint.getCreatedAt().toString())
                                .updatedAt(sprint.getUpdatedAt().toString())
                                .sprintGoal(sprint.getSprintGoal())
                                .status(sprint.getStatus().toString())
                                .build());
        }

        private boolean isUserAuthorized(Project project, String userId) {
                return projectProductOwnerRepository.existsByProjectIdAndUserId(project.getId(), userId)
                                || projectScrumMasterRepository.existsByProjectIdAndUserId(project.getId(), userId);
        }

        public SprintResponse editGoalAndDates(EditSprintGoalAndDatesRequest request) {
                Sprint sprint = sprintRepository.findById(request.getSprintId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Sprint not found"));

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String requesterId = authentication.getPrincipal().toString();

                if (!isUserAuthorized(sprint.getProject(), requesterId)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                        "You are not authorized to edit this sprint, only the product owner & scrum master can edit this sprint");
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

                if (request.getSprintGoal() != null) {
                        sprint.setSprintGoal(request.getSprintGoal());
                }

                if (request.getStartDate() != null) {
                        try {
                                sprint.setStartDate(LocalDateTime.parse(request.getStartDate(), formatter));
                        } catch (DateTimeParseException e) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid start date format");
                        }
                }

                if (request.getEndDate() != null) {
                        try {
                                sprint.setEndDate(LocalDateTime.parse(request.getEndDate(), formatter));
                        } catch (DateTimeParseException e) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid end date format");
                        }
                }

                Sprint saved = sprintRepository.save(sprint);

                return SprintResponse.builder()
                                .id(saved.getId().toString())
                                .projectId(saved.getProject().getId().toString())
                                .name(saved.getName())
                                .startDate(saved.getStartDate() != null ? saved.getStartDate().format(formatter) : null)
                                .endDate(saved.getEndDate() != null ? saved.getEndDate().format(formatter) : null)
                                .createdAt(saved.getCreatedAt().toString())
                                .updatedAt(saved.getUpdatedAt().toString())
                                .sprintGoal(saved.getSprintGoal())
                                .status(saved.getStatus().toString())
                                .build();
        }

        public SprintResponse startSprint(String sprintId) {
                Sprint sprint = sprintRepository.findById(sprintId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Sprint not found"));

                if (sprint.getStatus() == SprintStatus.IN_PROGRESS) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sprint is already in progress");
                }

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String requesterId = authentication.getPrincipal().toString();

                if (!isUserAuthorized(sprint.getProject(), requesterId)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                        "You are not authorized to start this sprint, only the product owner & scrum master of this project can start this sprint.");
                }

                LocalDateTime now = LocalDateTime.now();

                // ⛔ Throw error if now is after existing end date
                if (sprint.getEndDate() != null && now.isAfter(sprint.getEndDate())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Cannot start sprint because the end date is already in the past.");
                }

                sprint.setStatus(SprintStatus.IN_PROGRESS);
                sprint.setStartDate(now);

                Sprint saved = sprintRepository.save(sprint);

                DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

                return SprintResponse.builder()
                                .id(saved.getId())
                                .projectId(saved.getProject().getId())
                                .name(saved.getName())
                                .sprintGoal(saved.getSprintGoal())
                                .status(saved.getStatus().toString())
                                .startDate(saved.getStartDate() != null ? saved.getStartDate().format(formatter) : null)
                                .endDate(saved.getEndDate() != null ? saved.getEndDate().format(formatter) : null)
                                .createdAt(saved.getCreatedAt().toString())
                                .updatedAt(saved.getUpdatedAt().toString())
                                .build();
        }

        public CompleteSprintInfoResponse getCompleteSprintInfo(String sprintId) {
                Sprint sprint = sprintRepository.findById(sprintId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Sprint not found"));

                var backlogs = productBacklogRepository.findAllBySprintId(sprintId);

                int total = backlogs.size();
                int done = 0;

                for (var backlog : backlogs) {
                        if (backlog.getStatus() == ProductBacklogStatus.DONE) {
                                done++;
                        }
                }

                int notDone = total - done;

                return CompleteSprintInfoResponse.builder()
                                .totalBacklogs(total)
                                .doneBacklogs(done)
                                .notDoneBacklogs(notDone)
                                .build();
        }

        public SprintResponse completeSprint(String sprintId) {
                Sprint sprint = sprintRepository.findById(sprintId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Sprint not found"));

                if (sprint.getStatus() == SprintStatus.COMPLETED) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sprint is already completed");
                }

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String requesterId = authentication.getPrincipal().toString();

                if (!isUserAuthorized(sprint.getProject(), requesterId)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                        "You are not authorized to complete this sprint, only the product owner & scrum master of this project can complete this sprint.");
                }

                LocalDateTime now = LocalDateTime.now();

                // ⛔ Throw error if now is after existing end date
                if (now.isBefore(sprint.getStartDate())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Cannot complete sprint because now is before the start date.");
                }

                sprint.setStatus(SprintStatus.COMPLETED);
                sprint.setEndDate(now);

                Sprint saved = sprintRepository.save(sprint);

                DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

                return SprintResponse.builder()
                                .id(saved.getId().toString())
                                .projectId(saved.getProject().getId().toString())
                                .name(saved.getName())
                                .startDate(saved.getStartDate() != null ? saved.getStartDate().format(formatter) : null)
                                .endDate(saved.getEndDate() != null ? saved.getEndDate().format(formatter) : null)
                                .createdAt(saved.getCreatedAt().toString())
                                .updatedAt(saved.getUpdatedAt().toString())
                                .sprintGoal(saved.getSprintGoal())
                                .status(saved.getStatus().toString())
                                .build();
        }

        public Page<SprintResponse> searchSprint(String projectId, String keyword, Pageable pageable) {
                Page<Sprint> sprints = sprintRepository
                                .findByProjectIdAndNameContainingIgnoreCaseOrProjectIdAndSprintGoalContainingIgnoreCase(
                                                projectId, keyword, projectId, keyword, pageable);

                DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

                return sprints.map(sprint -> SprintResponse.builder()
                                .id(sprint.getId())
                                .projectId(sprint.getProject().getId())
                                .name(sprint.getName())
                                .sprintGoal(sprint.getSprintGoal())
                                .status(sprint.getStatus().toString())
                                .startDate(sprint.getStartDate() != null ? sprint.getStartDate().format(formatter)
                                                : null)
                                .endDate(sprint.getEndDate() != null ? sprint.getEndDate().format(formatter) : null)
                                .createdAt(sprint.getCreatedAt().toString())
                                .updatedAt(sprint.getUpdatedAt().toString())
                                .build());
        }

        public Page<TimelineSprintResponse> searchSprintInTimeline(String projectId, String keyword,
                        Pageable pageable) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String requesterId = authentication.getPrincipal().toString();

                FindUserResponse user = findUserOrThrow(requesterId);

                Page<Sprint> sprints = sprintRepository.findSprintsInTimelineWithStartAndEndDate(projectId, keyword,
                                pageable);

                DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

                return sprints.map(sprint -> {
                        int taskCount = productBacklogRepository.countBySprintIdAndAssigneeId(sprint.getId(),
                                        user.getId());

                        return TimelineSprintResponse.builder()
                                        .id(sprint.getId())
                                        .projectId(sprint.getProject().getId())
                                        .name(sprint.getName())
                                        .sprintGoal(sprint.getSprintGoal())
                                        .status(sprint.getStatus().toString())
                                        .startDate(sprint.getStartDate() != null
                                                        ? sprint.getStartDate().format(formatter)
                                                        : null)
                                        .endDate(sprint.getEndDate() != null ? sprint.getEndDate().format(formatter)
                                                        : null)
                                        .createdAt(sprint.getCreatedAt().toString())
                                        .updatedAt(sprint.getUpdatedAt().toString())
                                        .userTask(taskCount)
                                        .build();
                });
        }

        public SprintOverviewResponse getSprintOverview(String sprintId) {
                Sprint sprint = sprintRepository.findById(sprintId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Sprint not found"));

                List<ProductBacklog> backlogs = sprint.getProductBacklogs();

                int totalTasks = backlogs.size();
                int completedTasks = 0;
                int totalPoints = 0;
                int completedPoints = 0;

                for (ProductBacklog backlog : backlogs) {
                        totalPoints += backlog.getPoint();
                        if (backlog.getStatus() == ProductBacklogStatus.DONE) {
                                completedTasks++;
                                completedPoints += backlog.getPoint();
                        }
                }

                return SprintOverviewResponse.builder()
                                .startDate(sprint.getStartDate())
                                .endDate(sprint.getEndDate())
                                .sprintGoal(sprint.getSprintGoal())
                                .totalTasks(totalTasks)
                                .completedTasks(completedTasks)
                                .status(sprint.getStatus())
                                .totalPoints(totalPoints)
                                .completedPoints(completedPoints)
                                .build();
        }

        public List<UserTaskDistributionResponse> getTaskDistributionBySprint(String sprintId) {
                List<ProductBacklog> items = productBacklogRepository.findBySprintId(sprintId);
                Map<String, UserTaskDistributionResponse> distributionMap = new HashMap<>();

                for (ProductBacklog item : items) {
                        String userId = item.getAssigneeId() != null ? item.getAssigneeId() : "unassigned";
                        String name = userId.equals("unassigned") ? "Unassigned"
                                        : authenticationGrpcRepository.findUser(userId).getUsername();

                        distributionMap.putIfAbsent(userId, new UserTaskDistributionResponse(
                                        userId, name, 0, 0, 0, 0));

                        UserTaskDistributionResponse dist = distributionMap.get(userId);

                        dist.setTotalTasks(dist.getTotalTasks() + 1);
                        switch (item.getStatus()) {
                                case DONE -> dist.setDoneTasks(dist.getDoneTasks() + 1);
                                case INPROGRESS -> dist.setInProgressTasks(dist.getInProgressTasks() + 1);
                                case TODO -> dist.setTodoTasks(dist.getTodoTasks() + 1);
                        }
                }

                return new ArrayList<>(distributionMap.values());
        }

        private FindUserResponse findUserOrThrow(String userId) {
                try {
                        return authenticationGrpcRepository.findUser(userId);
                } catch (Exception e) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                                        "User with id " + userId + " not found");
                }
        }

}
