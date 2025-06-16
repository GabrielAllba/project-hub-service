package com.project_hub.project_hub_service.app.repository.postgres;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

import com.project_hub.project_hub_service.app.constants.ProductBacklogStatus;
import com.project_hub.project_hub_service.app.entity.ProductBacklog;

public interface ProductBacklogRepository extends JpaRepository<ProductBacklog, String> {
  Page<ProductBacklog> findAllByProjectId(String projectId, Pageable pageable);

  Optional<ProductBacklog> findByIdAndProjectId(String id, String projectId);

  Page<ProductBacklog> findByProjectIdAndSprintIsNull(String projectId, Pageable pageable);

  List<ProductBacklog> findAllByProjectIdAndSprintId(String projectId, @Nullable String sprintId);

  List<ProductBacklog> findAllBySprintId(String sprintId);

  @Query("""
          SELECT pb FROM ProductBacklog pb
          WHERE pb.project.id = :projectId
            AND pb.sprint IS NULL
            AND pb.id NOT IN (
                SELECT COALESCE(pb2.prevBacklog.id, '')
                FROM ProductBacklog pb2
                WHERE pb2.project.id = :projectId AND pb2.sprint IS NULL AND pb2.prevBacklog IS NOT NULL
            )
      """)
  ProductBacklog findLastBacklogByProjectIdAndSprintIsNull(@Param("projectId") String projectId);

  // Untuk unlink
  Optional<ProductBacklog> findByPrevBacklog(ProductBacklog prevBacklog);

  @Query("""
      SELECT pb FROM ProductBacklog pb
      WHERE pb.project.id = :projectId
        AND (:sprintId IS NULL AND pb.sprint IS NULL OR pb.sprint.id = :sprintId)
        AND pb.id NOT IN (
            SELECT COALESCE(child.prevBacklog.id, '') FROM ProductBacklog child
            WHERE child.project.id = :projectId
              AND (:sprintId IS NULL AND child.sprint IS NULL OR child.sprint.id = :sprintId)
        )
      """)
  ProductBacklog findLastBacklogByProjectIdAndSprintId(@Param("projectId") String projectId,
      @Param("sprintId") String sprintId);

  List<ProductBacklog> findByProductGoalId(String productGoalId);

  List<ProductBacklog> findBySprintIdAndStatusNot(String sprintId, ProductBacklogStatus status);

  List<ProductBacklog> findBySprintIdIn(List<String> sprintIds);

  List<ProductBacklog> findBySprintIdInAndUpdatedAtAfter(List<String> sprintIds, LocalDateTime after);

}
