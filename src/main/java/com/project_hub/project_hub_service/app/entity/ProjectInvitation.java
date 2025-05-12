package com.project_hub.project_hub_service.app.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_invitations", uniqueConstraints = @UniqueConstraint(columnNames = { "project_id",
        "invitee_id" }))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "invited_at", nullable = false)
    private LocalDateTime invitedAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status;

    @Column(name = "inviter_id", nullable = false)
    private String inviterId;

    @Column(name = "invitee_id", nullable = false)
    private String inviteeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
}
