package com.hapi.gridwatch.alert.repository;

import com.hapi.gridwatch.alert.model.WorkOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrderEntity, Long> {
}
