package com.project_hub.project_hub_service.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project_hub.project_hub_service.app.entity.Project;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {
}
