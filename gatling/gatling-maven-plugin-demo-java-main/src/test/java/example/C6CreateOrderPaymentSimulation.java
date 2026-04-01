package example;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * C6 – Dodaj płatność do zamówienia
 * Warmup simulation: 10 requests
 * Generates random order payment data
 */
public class C6CreateOrderPaymentSimulation extends Simulation {

    private static final String baseUrl = System.getProperty("baseUrl", "http://localhost:8080");
    private static final String endpoint = "/api/shop/order-payments";
    
    private static final String[] PROVIDERS = {"Stripe", "PayU", "Przelewy24", "PayPal", "Adyen"};
    private static final String[] STATUSES = {"PENDING", "AUTHORIZED", "CAPTURED"};
    private static final String[] CURRENCIES = {"PLN", "EUR", "USD"};

    private static final HttpProtocolBuilder httpProtocol = http.baseUrl(baseUrl)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .shareConnections();

    private static final ScenarioBuilder scenario = scenario("C6 Create Order Payment")
            .exec(session -> {
                // Generate random payment data
                long orderId = ThreadLocalRandom.current().nextLong(1, 2000000);
                long paymentMethodId = ThreadLocalRandom.current().nextLong(1, 5);
                String provider = PROVIDERS[ThreadLocalRandom.current().nextInt(PROVIDERS.length)];
                int amountCents = ThreadLocalRandom.current().nextInt(1000, 1000000);
                String currency = CURRENCIES[ThreadLocalRandom.current().nextInt(CURRENCIES.length)];
                String status = STATUSES[ThreadLocalRandom.current().nextInt(STATUSES.length)];
                String paidAt = OffsetDateTime.now().minusMinutes(ThreadLocalRandom.current().nextInt(1, 1440))
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

                return session
                    .set("orderId", orderId)
                    .set("paymentMethodId", paymentMethodId)
                    .set("provider", provider)
                    .set("amountCents", amountCents)
                    .set("currency", currency)
                    .set("status", status)
                    .set("paidAt", paidAt);
            })
            .exec(
                    http("POST /api/shop/order-payments")
                            .post(endpoint)
                            .body(StringBody("{\"orderId\":#{orderId},\"paymentMethodId\":#{paymentMethodId},\"provider\":\"#{provider}\",\"amountCents\":#{amountCents},\"currency\":\"#{currency}\",\"status\":\"#{status}\",\"paidAt\":\"#{paidAt}\"}"))
                            .check(status().is(200)));

    {
        setUp(scenario.injectOpen(rampUsers(10).during(10)))
                .protocols(httpProtocol);
    }
}
