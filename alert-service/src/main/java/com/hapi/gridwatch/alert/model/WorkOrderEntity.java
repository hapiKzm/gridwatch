package com.hapi.gridwatch.alert.model;

import com.hapi.gridwatch.alert.enums.WorkOrderStatusOption;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "work_order")
public class WorkOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String substationId;

    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_order_status", nullable = false)
    private WorkOrderStatusOption workOrderStatus;

    @CreationTimestamp
    @Column(nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public WorkOrderEntity() {
    }

    public WorkOrderEntity(String substationId, String reason, WorkOrderStatusOption workOrderStatus) {
        this.substationId = substationId;
        this.reason = reason;
        this.workOrderStatus = workOrderStatus;
    }

    public WorkOrderEntity(ReadingEntity readingEntity, WorkOrderStatusOption workOrderStatus, String reason) {
        this.substationId = readingEntity.getSubstationId();
        this.workOrderStatus = workOrderStatus;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getSubstationId() {
        return substationId;
    }
    public void setSubstationId(String substationId) {
        this.substationId = substationId;
    }

    public String getReason() {
        return reason;
    }
    public void setReason(String reason) {
        this.reason = reason;
    }

    public WorkOrderStatusOption getWorkOrderStatus() {
        return workOrderStatus;
    }
    public void setWorkOrderStatus(WorkOrderStatusOption workOrderStatus) {
        this.workOrderStatus = workOrderStatus;
    }
}
