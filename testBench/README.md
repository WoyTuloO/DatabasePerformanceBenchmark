# Shop CSV API

## Auto-reset i auto-seeding bazy

Przy starcie aplikacji możesz teraz sterować typem bazy i zestawem danych z poziomu `application.yaml`:

```yaml
app:
  database:
    type: postgres
  seed:
    enabled: true
    dataset: 1_000_000
    location: classpath*:database
    batch-size: 5000
    reset-before-load: true
```

### Jak działa bootstrap

1. aplikacja wybiera mechanizm po `app.database.type`
2. dla `postgres` sprawdza, czy istnieje schemat `shop`
3. jeśli trzeba, inicjalizuje go z `schema.txt`
4. wykonuje `TRUNCATE ... RESTART IDENTITY CASCADE`
5. ładuje CSV z katalogu `src/main/resources/database/<dataset>`
6. pliki są wykonywane według prefiksu liczbowego, np. `0_*`, potem `1_*`, `2_*`, itd.
7. po imporcie synchronizowane są sekwencje PostgreSQL

### Struktura datasetów

Przykład:

```text
src/main/resources/database/
└── 1_000_000/
    ├── 0_brands.csv
    ├── 0_categories.csv
    ├── 0_payment_methods.csv
    ├── 0_warehouses.csv
    ├── 1_customers_FIRST_TO_RUN.csv
    ├── 2_products_SECOND_TO_RUN.csv
    ├── 3_inventory_THIRD_TO_RUN.csv
    ├── 4_orders_FOURTH_TO_RUN.csv
    ├── 5_order_payments_5TH_TO_RUN.csv
    └── 6_order_items_6TH_TO_RUN.csv
```

Możesz dodawać kolejne zestawy, np. `100_000`, `500_000`, `1_000_000`, i przełączać je tylko przez `app.seed.dataset`.

## REST CRUD (JSON) pod Gatling

Aplikacja wystawia 24 endpointy `C1..D6` pod `/api/shop`.

Przyklady:

```bash
curl -X POST "http://localhost:8080/api/shop/orders/batch" \
  -H "Content-Type: application/json" \
  -d "[{\"customerId\":1,\"shippingCountry\":\"PL\",\"shippingCity\":\"Warszawa\",\"shippingPostalCode\":\"00-001\",\"shippingStreet\":\"Prosta\",\"shippingBuildingNo\":\"1\",\"shippingApartmentNo\":\"\",\"currency\":\"PLN\"}]"

curl "http://localhost:8080/api/shop/orders/1/items"

curl -X PATCH "http://localhost:8080/api/shop/products/1/active" \
  -H "Content-Type: application/json" \
  -d "{\"active\":false}"

curl -X DELETE "http://localhost:8080/api/shop/orders/1"
```



