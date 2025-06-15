package com.project_hub.project_hub_service.app.usecase;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

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
import com.project_hub.project_hub_service.app.dtos.res.SprintResponse;
import com.project_hub.project_hub_service.app.entity.Project;
import com.project_hub.project_hub_service.app.entity.Sprint;
import com.project_hub.project_hub_service.app.repository.postgres.ProductBacklogRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectProductOwnerRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectRepository;
import com.project_hub.project_hub_service.app.repository.postgres.ProjectScrumMasterRepository;
import com.project_hub.project_hub_service.app.repository.postgres.SprintRepository;

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

        public SprintResponse create(CreateSprintRequest request) {
                Project project = projectRepository.findById(request.getProjectId())
                                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

                Sprint sprint = Sprint.builder()
                                .project(project)
                                .name(request.getName())
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

        public Page<SprintResponse> getPaginatedSprintsTimelineByProjectId(String projectId, Pageable pageable) {
                if (!projectRepository.existsById(projectId)) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
                }

                Page<Sprint> sprintPage = sprintRepository.findWithStartAndEndDates(projectId, pageable);

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
                                        "You are not authorized to edit this sprint");
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
                                        "You are not authorized to start this sprint");
                }

                sprint.setStatus(SprintStatus.IN_PROGRESS);
                sprint.setStartDate(LocalDateTime.now());

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
                                        "You are not authorized to start this sprint");
                }

                sprint.setStatus(SprintStatus.COMPLETED);
                sprint.setStartDate(LocalDateTime.now());

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
}
