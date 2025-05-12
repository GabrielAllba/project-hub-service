package com.project_hub.project_hub_service.app.repository.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project_hub.project_hub_service.app.entity.ProjectInvitation;

@Repository
public interface ProjectInvitationRepository extends JpaRepository<ProjectInvitation, String> {
}
