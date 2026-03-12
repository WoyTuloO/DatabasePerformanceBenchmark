package com.example.performanceTesting.adapters.out.persistence.postgres.repository;

import com.example.performanceTesting.adapters.out.persistence.postgres.entity.CustomerAddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddressEntity, Long> {
}

