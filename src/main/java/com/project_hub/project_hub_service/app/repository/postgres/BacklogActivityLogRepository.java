package com.project_hub.project_hub_service.app.repository.postgres;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.project_hub.project_hub_service.app.entity.BacklogActivityLog;
import com.project_hub.project_hub_service.app.entity.ProductBacklog;

public interface BacklogActivityLogRepository extends JpaRepository<BacklogActivityLog, String> {
    Page<BacklogActivityLog> findByBacklogIdOrderByCreatedAtDesc(String backlogId, Pageable pageable);

    void deleteAllByBacklog(ProductBacklog backlog);

    void deleteAllByBacklogIn(List<ProductBacklog> backlogs);

}
