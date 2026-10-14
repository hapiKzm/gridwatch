package com.hapi.gridwatch.alert.repository;

import com.hapi.gridwatch.alert.model.ReadingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReadingRepository extends JpaRepository<ReadingEntity, UUID> {
}
