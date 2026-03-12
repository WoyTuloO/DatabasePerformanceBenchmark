package com.example.performanceTesting.adapters.out.persistence.postgres.repository;

import com.example.performanceTesting.adapters.out.persistence.postgres.entity.InventoryEntity;
import com.example.performanceTesting.adapters.out.persistence.postgres.entity.InventoryId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<InventoryEntity, InventoryId> {
}

