package com.project_hub.project_hub_service.app.repository.postgres;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project_hub.project_hub_service.app.entity.ProductGoal;

@Repository
public interface ProductGoalRepository extends JpaRepository<ProductGoal, String> {

    Page<ProductGoal> findByProjectId(String projectId, Pageable pageable);

    void deleteByProjectId(String projectId);

}
