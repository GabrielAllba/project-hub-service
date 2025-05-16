package com.project_hub.project_hub_service.app.repository.postgres;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project_hub.project_hub_service.app.entity.TeamMember;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, String> {
    List<TeamMember> findAllByUserId(String userId);
    void deleteAllByTeamId(String teamId);
}
