package com.project_hub.project_hub_service.app.usecase;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.project_hub.project_hub_service.app.dtos.req.AddTeamMemberRequest;
import com.project_hub.project_hub_service.app.dtos.req.CreateTeamRequest;
import com.project_hub.project_hub_service.app.dtos.res.TeamSummaryResponse;
import com.project_hub.project_hub_service.app.entity.Team;
import com.project_hub.project_hub_service.app.entity.TeamMember;
import com.project_hub.project_hub_service.app.repository.gRpc.AuthenticationGrpcRepository;
import com.project_hub.project_hub_service.app.repository.postgres.TeamMemberRepository;
import com.project_hub.project_hub_service.app.repository.postgres.TeamRepository;

import authenticationservice.AuthenticationServiceOuterClass.FindUserResponse;

@Service
public class TeamUseCase {

    private final AuthenticationGrpcRepository authenticationGrpcRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    public TeamUseCase(TeamRepository teamRepository, TeamMemberRepository teamMemberRepository,
            AuthenticationGrpcRepository authRepository) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.authenticationGrpcRepository = authRepository;
    }

    public Team createTeam(CreateTeamRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String requesterId = authentication.getPrincipal().toString();

        Team team = Team.builder()
                .name(request.getName())
                .creatorId(requesterId)
                .build();

        Team savedTeam = teamRepository.save(team);

        return savedTeam;
    }

    public TeamMember addMember(String teamId, AddTeamMemberRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String requesterId = authentication.getPrincipal().toString();

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));

        if (!team.getCreatorId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only team creator can add members");
        }

        if (requesterId.equals(request.getUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The creator already associated with the member");
        }

        FindUserResponse user;
        try {
            user = authenticationGrpcRepository.findUser(request.getUserId());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "User with id " + request.getUserId() + " not found");
        }
        try {
            TeamMember member = TeamMember.builder()
                    .team(team)
                    .userId(user.getId())
                    .build();

            return teamMemberRepository.save(member);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User already be a member of this project");
        }
    }

    public Page<TeamSummaryResponse> getMyTeams(Pageable pageable) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getPrincipal().toString();

        List<String> teamMemberIds = teamMemberRepository.findAllByUserId(userId)
                .stream()
                .map(pm -> pm.getTeam().getId())
                .toList();

        List<String> createdTeamIds = teamRepository.findAllByCreatorId(userId)
                .stream()
                .map(Team::getId)
                .toList();

        Set<String> allTeamIds = new HashSet<>();
        allTeamIds.addAll(teamMemberIds);
        allTeamIds.addAll(createdTeamIds);

        Page<Team> teamPage = teamRepository.findAllByIdIn(allTeamIds, pageable);

        return teamPage.map(team -> TeamSummaryResponse.builder()
                .teamId(team.getId())
                .name(team.getName())
                .build());

    }

    @Transactional
    public void deleteTeam(String teamId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getPrincipal().toString();

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));

        if (!team.getCreatorId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator can delete the team");
        }

        teamMemberRepository.deleteAllByTeamId(teamId);
        teamRepository.delete(team);
    }

}
