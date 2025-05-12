package com.project_hub.project_hub_service.app.repository.postgres;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project_hub.project_hub_service.app.entity.ProjectMember;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, String> {
    Optional<ProjectMember> findByProjectIdAndUserId(String projectId, String userId);
}
