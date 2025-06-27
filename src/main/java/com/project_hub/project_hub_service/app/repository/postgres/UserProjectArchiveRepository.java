package com.project_hub.project_hub_service.app.repository.postgres;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project_hub.project_hub_service.app.entity.UserProjectArchive;

@Repository
public interface UserProjectArchiveRepository extends JpaRepository<UserProjectArchive, String> {

    boolean existsByUserIdAndProject_Id(String userId, String projectId);

    void deleteByUserIdAndProject_Id(String userId, String projectId);

    List<UserProjectArchive> findAllByUserId(String userId);

    List<UserProjectArchive> findAllByUserIdAndProject_IdIn(String userId, List<String> projectIds);

    List<UserProjectArchive> findAllByUserIdAndProject_IdIn(String userId, Collection<String> projectIds);

    Page<UserProjectArchive> findAllByUserId(String userId, Pageable pageable);

    Optional<UserProjectArchive> findByUserIdAndProject_Id(String userId, String projectId);

}
