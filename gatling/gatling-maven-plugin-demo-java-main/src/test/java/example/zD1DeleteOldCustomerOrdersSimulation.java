package example;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

/**
 * D1 – Usuń stare zamówienia klienta
 * Warmup simulation: 10 requests
 * Generates random customerId with fixed ISO-8601 cutoffDate
 */
public class zD1DeleteOldCustomerOrdersSimulation extends Simulation {

    private static final String baseUrl = System.getProperty("baseUrl", "http://localhost:8080");
    private static final String CUTOFF_DATE = "2025-01-01T00:00:00+01:00"; // Fixed ISO-8601 date

    private static final HttpProtocolBuilder httpProtocol = http.baseUrl(baseUrl)
            .acceptHeader("application/json")
            .shareConnections();

    private static final ScenarioBuilder scenario = scenario("D1 Delete Old Customer Orders")
            .exec(session -> {
                // Generate random customerId with fixed cutoffDate
                long customerId = ThreadLocalRandom.current().nextLong(1, 10001);
                // URL encode the date
                String encodedDate = URLEncoder.encode(CUTOFF_DATE, StandardCharsets.UTF_8);
                
                return session
                    .set("customerId", customerId)
                    .set("cutoffDate", encodedDate);
            })
            .exec(
                    http("DELETE /api/shop/customers/{customerId}/old-orders")
                            .delete("/api/shop/customers/#{customerId}/old-orders?cutoffDate=#{cutoffDate}")
                            .check(status().in(200, 404))); // 404 OK if no old orders found

    {
        setUp(scenario.injectOpen(rampUsers(10).during(10)))
                .protocols(httpProtocol)
                .assertions(
                        global().responseTime().mean().lt(1000));
    }
}

