package com.project_hub.project_hub_service.app.dtos.res;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.project_hub.project_hub_service.app.constants.BacklogActivityType;
import com.project_hub.project_hub_service.app.constants.ProductBacklogPriority;
import com.project_hub.project_hub_service.app.constants.ProductBacklogStatus;
import com.project_hub.project_hub_service.app.entity.ProductBacklog;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacklogActivityLogResponse {
        
    private String id;
    private String backlogId;
    private String userId;
    private String username;
    private BacklogActivityType activityType;
    private String description;
    private String oldValue;
    private String newValue;
    private LocalDateTime createdAt;
}
