package com.project_hub.project_hub_service.app.repository.postgres;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project_hub.project_hub_service.app.entity.ProjectDeveloper;

@Repository
public interface ProjectDeveloperRepository extends JpaRepository<ProjectDeveloper, String> {
    Optional<ProjectDeveloper> findByProjectIdAndUserId(String projectId, String userId);

    List<ProjectDeveloper> findAllByUserId(String userId);

    List<ProjectDeveloper> findAllByProjectId(String projectId);

    boolean existsByProjectIdAndUserId(String projectId, String userId);

    long countByProjectId(String projectId);

}
