package com.project_hub.project_hub_service.app.repository.postgres;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project_hub.project_hub_service.app.constants.SprintStatus;
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

  List<Sprint> findAllByProjectIdAndStatus(String projectId, SprintStatus status);

  @Query("""
          SELECT s FROM Sprint s
          WHERE s.project.id = :projectId
            AND (
                (s.startDate IS NOT NULL AND s.startDate BETWEEN :start AND :end)
                OR (s.endDate IS NOT NULL AND s.endDate BETWEEN :start AND :end)
                OR (s.startDate IS NOT NULL AND s.endDate IS NOT NULL AND s.startDate <= :start AND s.endDate >= :end)
                OR (s.startDate IS NOT NULL AND s.endDate IS NULL AND s.startDate <= :end)
            )
      """)
  List<Sprint> findByProjectIdAndDateRange(
      @Param("projectId") String projectId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  Page<Sprint> findByProjectIdAndNameContainingIgnoreCaseOrProjectIdAndSprintGoalContainingIgnoreCase(
      String projectId1, String nameKeyword, String projectId2, String goalKeyword,
      Pageable pageable);

  @Query("""
          SELECT s FROM Sprint s
          WHERE s.project.id = :projectId
            AND (
              (s.startDate BETWEEN :start AND :end)
              OR (s.endDate BETWEEN :start AND :end)
              OR (s.startDate <= :start AND s.endDate >= :end)
            )
          ORDER BY s.startDate DESC
      """)
  Page<Sprint> findWithDateRangeAndPagination(
      @Param("projectId") String projectId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      Pageable pageable);

  void deleteByProjectId(String projectId);

  List<Sprint> findByProjectId(String projectId);

  @Query("""
          SELECT s FROM Sprint s
          WHERE s.project.id = :projectId
            AND s.startDate IS NOT NULL
            AND s.endDate IS NOT NULL
            AND (
              LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(s.sprintGoal) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
          ORDER BY s.startDate DESC
      """)
  Page<Sprint> findSprintsInTimelineWithStartAndEndDate(
      @Param("projectId") String projectId,
      @Param("keyword") String keyword,
      Pageable pageable);

}
