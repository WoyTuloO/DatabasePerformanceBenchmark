package com.example.performanceTesting.adapters.out.persistence.postgres.repository;

import com.example.performanceTesting.adapters.out.persistence.postgres.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
}

