package example;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.util.concurrent.ThreadLocalRandom;

/**
 * U6 – Anuluj zamówienia opłacone daną metodą
 * Warmup simulation: 10 requests
 */
public class U6CancelOrdersByPaymentMethodSimulation extends Simulation {

    private static final String baseUrl = System.getProperty("baseUrl", "http://localhost:8080");

    private static final String csvFile = "data/u6_cancel_orders_by_payment_method.csv";

    private static final HttpProtocolBuilder httpProtocol = http.baseUrl(baseUrl)
            .acceptHeader("application/json")
            .shareConnections();

    private static final FeederBuilder<String> feeder = csv(csvFile);


    private static final ScenarioBuilder scenario = scenario("U6 Cancel Orders By Payment Method")
            .feed(feeder)
            .exec(
                    http("POST /api/shop/payment-methods/{code}/cancel-orders")
                            .post("/api/shop/payment-methods/#{code}/cancel-orders")
                            .check(status().is(200)));

    {
        setUp(scenario.injectOpen(atOnceUsers(1)))
                .protocols(httpProtocol);
    }
}

