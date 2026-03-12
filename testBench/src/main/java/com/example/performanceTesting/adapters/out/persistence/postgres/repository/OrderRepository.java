package com.example.performanceTesting.adapters.out.persistence.postgres.repository;

import com.example.performanceTesting.adapters.out.persistence.postgres.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
}

