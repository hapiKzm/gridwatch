package com.hapi.gridwatch.notifier.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "sent_notification")
public class SentNotificationEntity {

    @Id
    private Long workOrderId;

    @CreationTimestamp
    @Column(nullable = false)
    private Instant sentAt;

    protected SentNotificationEntity() {
    }

    public SentNotificationEntity(Long workOrderId) {
        this.workOrderId = workOrderId;
    }

    public Long getWorkOrderId() {
        return workOrderId;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
