package com.project_hub.project_hub_service.app.repository.postgres;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project_hub.project_hub_service.app.entity.ProjectInvitation;

@Repository
public interface ProjectInvitationRepository extends JpaRepository<ProjectInvitation, String> {
    Page<ProjectInvitation> findByInviteeId(String inviteeId, Pageable pageable);

}
