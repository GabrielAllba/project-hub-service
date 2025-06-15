package com.project_hub.project_hub_service.app.repository.postgres;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project_hub.project_hub_service.app.entity.Sprint;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, String> {
  Page<Sprint> findByProjectId(String projectId, Pageable pageable);

  @Query("""
          SELECT s FROM Sprint s
          WHERE s.project.id = :projectId
            AND s.status <> 'COMPLETED'
          ORDER BY
              CASE s.status
                  WHEN 'IN_PROGRESS' THEN 0
                  WHEN 'NOT_STARTED' THEN 1
                  ELSE 2
              END,
              s.createdAt DESC
      """)
  Page<Sprint> findActiveSprintsByProjectOrdered(@Param("projectId") String projectId, Pageable pageable);

  @Query("""
          SELECT s FROM Sprint s
          WHERE s.project.id = :projectId
            AND s.status <> 'COMPLETED'
            AND s.status <> 'NOT_STARTED'
          ORDER BY
              CASE s.status
                  WHEN 'IN_PROGRESS' THEN 0
                  ELSE 1
              END,
              s.createdAt DESC
      """)
  Page<Sprint> findInProgressSprintsByProjectOrdered(@Param("projectId") String projectId, Pageable pageable);

  @Query("""
          SELECT s FROM Sprint s
          WHERE s.project.id = :projectId
            AND s.startDate IS NOT NULL
            AND s.endDate IS NOT NULL
          ORDER BY s.startDate DESC
      """)
  Page<Sprint> findWithStartAndEndDates(
      @Param("projectId") String projectId,
      Pageable pageable);
}
