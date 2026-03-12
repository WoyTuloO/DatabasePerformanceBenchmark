package com.example.performanceTesting.adapters.out.persistence.postgres.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "inventory", schema = "shop")
public class InventoryEntity {

    @EmbeddedId
    private InventoryId id;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}

