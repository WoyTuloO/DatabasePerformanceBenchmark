package example;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.util.concurrent.ThreadLocalRandom;

/**
 * C1 – Utwórz zamówienie
 * Warmup simulation: 10 requests
 * IDEMPOTENT with D2DeleteCartSimulation - creates NEW orders that D2 will delete
 * Generates random data and saves created order_id to file
 */
public class C1BatchCreateOrderSimulation extends Simulation {


    private static final String[] CITIES = {"Warsaw", "Krakow", "Wroclaw", "Poznan", "Gdansk", "Szczecin", "Lodz", "Bydgoszcz", "Lublin", "Katowice"};
    private static final String[] STREETS = {"Marszalkowska", "Florianska", "Rynek", "Stary Rynek", "Dluga", "Bohaterow", "Piotrkowska", "Gdanska", "Krakowskie Przedmiescie", "Wolnosci"};

    private static final String baseUrl = System.getProperty("baseUrl", "http://localhost:8080");
    private static final String endpoint = "/api/shop/orders/batch";
    private static final int BATCH_SIZE = 50; // Rozmiar paczki

    private static final HttpProtocolBuilder httpProtocol = http.baseUrl(baseUrl)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .shareConnections();
    private static final ScenarioBuilder scenario = scenario("C1 Batch Create Orders")
            .exec(session -> {
                StringBuilder jsonBuilder = new StringBuilder("[");
                for (int i = 0; i < BATCH_SIZE; i++) {
                    long customerId = ThreadLocalRandom.current().nextLong(1, 1000000);
                    String city = CITIES[ThreadLocalRandom.current().nextInt(CITIES.length)];
                    String street = STREETS[ThreadLocalRandom.current().nextInt(STREETS.length)];
                    String postalCode = String.format("%02d-%03d", 10 + i, 100 + i * 2);

                    jsonBuilder.append(String.format(
                            "{\"customerId\":%d,\"shippingCountry\":\"Poland\",\"shippingCity\":\"%s\",\"shippingPostalCode\":\"%s\",\"shippingStreet\":\"%s\",\"shippingBuildingNo\":\"1\",\"shippingApartmentNo\":\"1\",\"currency\":\"PLN\"}",
                            customerId, city, postalCode, street
                    ));

                    if (i < BATCH_SIZE - 1) jsonBuilder.append(",");
                }
                jsonBuilder.append("]");

                return session.set("batchBody", jsonBuilder.toString());
            })
            .exec(
                    http("POST /api/shop/orders/batch")
                            .post(endpoint)
                            .body(StringBody("#{batchBody}"))
                            .check(status().is(200))
            );

    {
        setUp(scenario.injectOpen(atOnceUsers(10)))
                .protocols(httpProtocol);
    }
}
