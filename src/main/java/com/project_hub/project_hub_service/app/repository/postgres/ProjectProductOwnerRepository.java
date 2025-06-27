package com.project_hub.project_hub_service.app.repository.postgres;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project_hub.project_hub_service.app.entity.ProjectProductOwner;

@Repository
public interface ProjectProductOwnerRepository extends JpaRepository<ProjectProductOwner, String> {
    List<ProjectProductOwner> findAllByUserId(String userId);

    List<ProjectProductOwner> findAllByProjectId(String projectId);

    boolean existsByProjectIdAndUserId(String projectId, String userId);

    void deleteByProjectId(String projectId);

    List<ProjectProductOwner> findAllByUserIdAndProject_IdIn(String userId, Collection<String> projectIds);

}
