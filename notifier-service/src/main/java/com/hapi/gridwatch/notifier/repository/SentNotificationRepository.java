package com.hapi.gridwatch.notifier.repository;

import com.hapi.gridwatch.notifier.model.SentNotificationEntity;
import com.hapi.gridwatch.notifier.model.SentNotificationPkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SentNotificationRepository extends JpaRepository<SentNotificationEntity, SentNotificationPkEntity> {

}
