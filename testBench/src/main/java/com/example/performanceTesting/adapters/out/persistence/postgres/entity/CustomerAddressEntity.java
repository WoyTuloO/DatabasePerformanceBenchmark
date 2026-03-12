package com.example.performanceTesting.adapters.out.persistence.postgres.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "customer_addresses", schema = "shop")
public class CustomerAddressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long addressId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "address_label", nullable = false)
    private String addressLabel;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private String city;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @Column(nullable = false)
    private String street;

    @Column(name = "building_no", nullable = false)
    private String buildingNo;

    @Column(name = "apartment_no")
    private String apartmentNo;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}

