package example;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.util.concurrent.ThreadLocalRandom;

/**
 * U2 – Zmień status zamówienia na podstawie payment_id
 * Warmup simulation: 10 requests
 */
public class U2UpdateOrderStatusByPaymentSimulation extends Simulation {

    private static final String baseUrl = System.getProperty("baseUrl", "http://localhost:8080");
    private static final String csvFile = "data/u2_update_order_status_by_payment.csv";

    private static final String[] STATUSES = {"NEW", "PAID", "SHIPPED", "CANCELLED", "RETURNED"};

    private static final HttpProtocolBuilder httpProtocol = http.baseUrl(baseUrl)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .shareConnections();

    private static final ScenarioBuilder scenario = scenario("U2 Update Order Status By Payment")
            .exec(session -> {
                long orderPaymentId = ThreadLocalRandom.current().nextLong(1, 500001);
                String status = STATUSES[ThreadLocalRandom.current().nextInt(STATUSES.length)];
                return session.set("orderPaymentId", orderPaymentId).set("status", status);
            })
            .exec(
                    http("PATCH /api/shop/order-payments/{orderPaymentId}/order-status")
                            .patch("/api/shop/order-payments/#{orderPaymentId}/order-status")
                            .body(StringBody("{\"status\":\"#{status}\"}"))
                            .check(status().is(200)));

    {
        setUp(scenario.injectOpen(rampUsers(10).during(10)))
                .protocols(httpProtocol);
    }
}

