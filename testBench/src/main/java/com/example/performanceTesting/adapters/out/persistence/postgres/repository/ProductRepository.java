package com.example.performanceTesting.adapters.out.persistence.postgres.repository;

import com.example.performanceTesting.adapters.out.persistence.postgres.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
}

