package com.example.performanceTesting.shop.application;
import com.example.performanceTesting.bootstrap.config.DatabaseType;
public interface DatabaseAwareShopService extends ShopService {
    DatabaseType type();
}