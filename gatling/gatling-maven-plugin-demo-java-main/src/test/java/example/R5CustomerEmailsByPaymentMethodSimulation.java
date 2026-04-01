package example;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * R5 – Maile klientów płacących daną metodą
 * Warmup simulation: 10 requests
 * Generuje losowe payment method codes z predefiniowanej listy
 */
public class R5CustomerEmailsByPaymentMethodSimulation extends Simulation {

    private static final String baseUrl = System.getProperty("baseUrl", "http://localhost:8080");
    private static final String[] PAYMENT_CODES = {"BLIK", "CARD", "TRANSFER", "ONLINE_TRANSFER", "COD"};

    private static final HttpProtocolBuilder httpProtocol = http.baseUrl(baseUrl)
            .acceptHeader("application/json")
            .shareConnections();

    private static final ScenarioBuilder scenario = scenario("R5 Customer Emails By Payment Method")
            .exec(session -> {
                String code = PAYMENT_CODES[ThreadLocalRandom.current().nextInt(PAYMENT_CODES.length)];
                return session.set("code", code);
            })
            .exec(
                    http("GET /api/shop/customers/by-payment-method/{code}/emails")
                            .get("/api/shop/customers/by-payment-method/#{code}/emails")
                            .check(status().in(200, 404)));

    {
        setUp(scenario.injectOpen(rampUsers(10).during(10)))
                .protocols(httpProtocol)
                .assertions(
                        global().responseTime().mean().lt(1000));
    }
}

