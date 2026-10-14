package com.hapi.gridwatch.alert.repository;

import com.hapi.gridwatch.alert.model.ReadingEntity;
import com.hapi.gridwatch.alert.model.WorkOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkOrderRepository extends JpaRepository<WorkOrderEntity, Long> {
}
