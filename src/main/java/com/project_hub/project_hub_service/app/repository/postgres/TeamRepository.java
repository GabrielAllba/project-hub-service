package com.project_hub.project_hub_service.app.repository.postgres;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project_hub.project_hub_service.app.entity.Team;

@Repository
public interface TeamRepository extends JpaRepository<Team, String> {
        List<Team> findAllByCreatorId(String creatorId);

        Page<Team> findAllByIdIn(Collection<String> ids, Pageable pageable);
}
