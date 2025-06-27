package com.project_hub.project_hub_service.app.repository.postgres;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project_hub.project_hub_service.app.constants.InvitationStatus;
import com.project_hub.project_hub_service.app.constants.ProjectRole;
import com.project_hub.project_hub_service.app.entity.ProjectInvitation;

@Repository
public interface ProjectInvitationRepository extends JpaRepository<ProjectInvitation, String> {
        Page<ProjectInvitation> findByInviteeId(String inviteeId, Pageable pageable);

        Optional<ProjectInvitation> findByProjectIdAndInviteeIdAndRole(String projectId, String inviteeId,
                        ProjectRole role);

        Optional<ProjectInvitation> findByProjectIdAndInviteeId(String projectId, String inviteeId);

        boolean existsByProjectIdAndInviteeIdAndRoleAndStatus(
                        String projectId,
                        String inviteeId,
                        ProjectRole role,
                        InvitationStatus status);

        Optional<ProjectInvitation> findFirstByProjectIdAndInviteeIdAndStatus(
                        String projectId,
                        String inviteeId,
                        InvitationStatus status);

        void deleteByProjectId(String projectId);

}
