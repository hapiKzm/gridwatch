package com.hapi.gridwatch.notifier.model;

import com.hapi.gridwatch.notifier.types.WorkOrderType;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;


@Embeddable
public class SentNotificationPkEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "work_order_id")
    private Long workOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_order_status")
    private WorkOrderType workOrderType;

    protected SentNotificationPkEntity() {
    }

    public SentNotificationPkEntity(Long workOrderId, WorkOrderType workOrderType) {
        this.workOrderId = workOrderId;
        this.workOrderType = workOrderType;
    }

    public Long getWorkOrderId() {
        return workOrderId;
    }
    public void setWorkOrderId(Long workOrderId) {
        this.workOrderId = workOrderId;
    }

    public WorkOrderType getWorkOrderStatus() {
        return workOrderType;
    }
    public void setWorkOrderStatus(WorkOrderType workOrderType) {
        this.workOrderType = workOrderType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SentNotificationPkEntity that = (SentNotificationPkEntity) o;
        return Objects.equals(workOrderId, that.workOrderId) && workOrderType == that.workOrderType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(workOrderId, workOrderType);
    }
}
