package com.project_hub.project_hub_service.app.repository.postgres;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project_hub.project_hub_service.app.entity.ProjectScrumMaster;

@Repository
public interface ProjectScrumMasterRepository extends JpaRepository<ProjectScrumMaster, String> {
    List<ProjectScrumMaster> findAllByUserId(String userId);

    List<ProjectScrumMaster> findAllByProjectId(String projectId);

    boolean existsByProjectIdAndUserId(String projectId, String userId);
}
