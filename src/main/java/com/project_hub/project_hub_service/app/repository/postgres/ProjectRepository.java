package com.project_hub.project_hub_service.app.repository.postgres;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project_hub.project_hub_service.app.entity.Project;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {
    List<Project> findAllByProductOwnerId(String productOwnerId);
    List<Project> findAllByScrumMasterId(String scrumMasterId);
    Page<Project> findAllByIdIn(Collection<String> ids, Pageable pageable);
    boolean existsByIdAndProductOwnerId(String id, String productOwnerId);
    boolean existsByIdAndScrumMasterId(String id, String scrumMasterId);
}
