package com.project_hub.project_hub_service.app.repository.postgres;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.project_hub.project_hub_service.app.entity.ProductBacklog;

public interface ProductBacklogRepository extends JpaRepository<ProductBacklog, String> {
    Page<ProductBacklog> findAllByProjectId(String projectId, Pageable pageable);
}
