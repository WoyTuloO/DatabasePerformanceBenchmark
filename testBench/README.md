# Shop CSV API

Aplikacja wystawia 24 endpointy `C1..D6` pod `/api/postgres/shop`.

- `Content-Type: text/csv`
- `Accept: text/csv`
- request body moze miec wiele wierszy (batch)
- odpowiedz jest CSV z naglowkiem

24 endpointy CRUD (C1..D6) sa pod `/api/postgres/shop/*`.

## Import 6 plikow startowych (kolejnosc)

1. `POST /api/postgres/shop/seed/customers`
2. `POST /api/postgres/shop/seed/products`
3. `POST /api/postgres/shop/seed/inventory`
4. `POST /api/postgres/shop/seed/order-payments` (dla Twojego pliku `4_order_items_*`, bo dane maja format platnosci)
5. `POST /api/postgres/shop/seed/orders`
6. `POST /api/postgres/shop/seed/order-payments`

Przyklad:

```bash
curl -X POST "http://localhost:8080/api/postgres/shop/c3/customers" \
  -H "Content-Type: text/csv" \
  -H "Accept: text/csv" \
  --data-binary "user1@example.com,hash_1,Anna,Nowak,+48123123123"
```

Test mapowania endpointow:

```bash
./mvnw.cmd -pl testBench -Dtest=ShopControllerMappingsTest test
```

## REST CRUD (JSON) pod Gatling

Dla testow wydajnosciowych masz tez normalne endpointy CRUD (GET/POST/PUT/PATCH/DELETE) pod tym samym prefiksem `/api/postgres/shop`.

Przyklady:

```bash
curl -X POST "http://localhost:8080/api/postgres/shop/orders" \
  -H "Content-Type: application/json" \
  -d "{\"customerId\":1,\"shippingCountry\":\"PL\",\"shippingCity\":\"Warszawa\",\"shippingPostalCode\":\"00-001\",\"shippingStreet\":\"Prosta\",\"shippingBuildingNo\":\"1\",\"shippingApartmentNo\":\"\",\"currency\":\"PLN\"}"

curl "http://localhost:8080/api/postgres/shop/orders?customerId=1&limit=50&offset=0"

curl -X PATCH "http://localhost:8080/api/postgres/shop/orders/1/status?status=PAID"

curl -X DELETE "http://localhost:8080/api/postgres/shop/order-items/1/1"
```



