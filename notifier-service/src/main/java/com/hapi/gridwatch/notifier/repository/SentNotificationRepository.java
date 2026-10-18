package com.hapi.gridwatch.notifier.repository;

import com.hapi.gridwatch.notifier.model.SentNotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SentNotificationRepository extends JpaRepository<SentNotificationEntity, Long> {
}
