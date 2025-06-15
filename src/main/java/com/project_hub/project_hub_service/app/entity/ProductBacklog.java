package com.project_hub.project_hub_service.app.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.project_hub.project_hub_service.app.constants.ProductBacklogPriority;
import com.project_hub.project_hub_service.app.constants.ProductBacklogStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product_backlogs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductBacklog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_goal_id")
    private ProductGoal productGoal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prev_backlog_id")
    private ProductBacklog prevBacklog;

    @Column(nullable = false)
    private String title;

    @Builder.Default
    @Column(nullable = false)
    private int point = 0;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductBacklogPriority priority;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductBacklogStatus status;

    @Column(name = "creator_id")
    private String creatorId;

    @Column(name = "assignee_id", nullable = true)
    private String assigneeId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
