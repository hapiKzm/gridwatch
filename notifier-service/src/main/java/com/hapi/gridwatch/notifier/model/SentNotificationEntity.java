package com.hapi.gridwatch.notifier.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "sent_notification")
public class SentNotificationEntity {

    @EmbeddedId
    private SentNotificationPkEntity id;

    @CreationTimestamp
    @Column(nullable = false)
    private Instant sentAt;


    public SentNotificationEntity() {}

    public SentNotificationPkEntity getId() {
        return id;
    }
    public void setId(SentNotificationPkEntity id) {
        this.id = id;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }
}
