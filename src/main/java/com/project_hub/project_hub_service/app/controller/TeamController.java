package com.project_hub.project_hub_service.app.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project_hub.project_hub_service.app.dtos.req.AddTeamMemberRequest;
import com.project_hub.project_hub_service.app.dtos.req.CreateTeamRequest;
import com.project_hub.project_hub_service.app.dtos.res.TeamSummaryResponse;
import com.project_hub.project_hub_service.app.entity.Team;
import com.project_hub.project_hub_service.app.entity.TeamMember;
import com.project_hub.project_hub_service.app.usecase.TeamUseCase;
import com.project_hub.project_hub_service.misc.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/team")
@Tag(name = "Teams", description = "Manage teams and their members")
@SecurityRequirement(name = "bearerAuth")
public class TeamController {

    private final TeamUseCase teamUseCase;

    public TeamController(TeamUseCase teamUseCase) {
        this.teamUseCase = teamUseCase;
    }

    @PostMapping
    @Operation(summary = "Create a new team")
    public ResponseEntity<BaseResponse<Team>> createTeam(@RequestBody CreateTeamRequest request) {
        Team team = teamUseCase.createTeam(request);
        return ResponseEntity.ok(new BaseResponse<>("success", "Team created successfully", team));
    }

    @PostMapping("/{teamId}/member")
    @Operation(summary = "Add a member to a team")
    public ResponseEntity<BaseResponse<TeamMember>> addMember(
            @PathVariable String teamId,
            @RequestBody AddTeamMemberRequest request) {
        TeamMember member = teamUseCase.addMember(teamId, request);

        BaseResponse<TeamMember> response = new BaseResponse<>(
                "success",
                "Team member added successfully",
                member);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    @Operation(summary = "Get teams authorized user belong to")
    public ResponseEntity<BaseResponse<Page<TeamSummaryResponse>>> getMyTeams(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<TeamSummaryResponse> teams = teamUseCase.getMyTeams(pageable);

        BaseResponse<Page<TeamSummaryResponse>> response = new BaseResponse<>(
                "success",
                "Teams retrieved successfully",
                teams);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{teamId}")
    @Operation(summary = "Delete a team (only allowed for the creator)")
    public ResponseEntity<BaseResponse<Void>> deleteTeam(@PathVariable String teamId) {
        teamUseCase.deleteTeam(teamId);
        return ResponseEntity.ok(new BaseResponse<>("success", "Team deleted successfully", null));
    }

}
