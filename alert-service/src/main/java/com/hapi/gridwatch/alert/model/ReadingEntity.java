package com.hapi.gridwatch.alert.model;

import com.hapi.gridwatch.alert.enums.SensorType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "reading")
public class ReadingEntity {

    @Id
    private UUID id;
    @Column(nullable = false)
    private String substationId;

    @Enumerated(EnumType.STRING)
    @Column(name="sensor_type", nullable = false)
    private SensorType sensorType;

    @Column(nullable = false)
    private Double value;

    @Column(nullable = false)
    private Instant measuredAt;

    @CreationTimestamp
    @Column(nullable = false)
    private Instant createdAt;

    @UpdateTimestamp

    private Instant updatedAt;


    public ReadingEntity() {
    }


    public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
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

    public SensorType getSensorType() {
        return sensorType;
    }
    public void setSensorType(SensorType sensorType) {
        this.sensorType = sensorType;
    }

    public Double getValue() {
        return value;
    }
    public void setValue(Double value) {
        this.value = value;
    }

    public Instant getMeasuredAt() {
        return measuredAt;
    }
    public void setMeasuredAt(Instant measuredAt) {
        this.measuredAt = measuredAt;
    }
}
