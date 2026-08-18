package com.nexusenroll.enrollment.db.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_audit_logs")
public class AdminAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    // Soft reference to Admin in Auth Service
    @Column(name = "admin_id", nullable = false, length = 36)
    private String adminId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AdminAction actionType;

    // e.g., studentId or courseId string
    @Column(nullable = false, length = 255)
    private String targetEntityId;

    @Column(columnDefinition = "TEXT")
    private String details;

    private LocalDateTime performedAt;

    public AdminAuditLogEntity() {
    }

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public AdminAction getActionType() {
        return actionType;
    }

    public void setActionType(AdminAction actionType) {
        this.actionType = actionType;
    }

    public String getTargetEntityId() {
        return targetEntityId;
    }

    public void setTargetEntityId(String targetEntityId) {
        this.targetEntityId = targetEntityId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getPerformedAt() {
        return performedAt;
    }

    public void setPerformedAt(LocalDateTime performedAt) {
        this.performedAt = performedAt;
    }
}