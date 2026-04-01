package example;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * C3 – Utwórz klienta
 * Warmup simulation: 10 requests
 * IDEMPOTENT with D4DeleteCustomerSimulation - creates customers that D4 will delete
 * Generates random customer data and saves created customer_id to file
 */
public class C3CreateCustomerSimulation extends Simulation {

    private static final String baseUrl = System.getProperty("baseUrl", "http://localhost:8080");
    private static final String endpoint = "/api/shop/customers";

    private static final String[] FIRST_NAMES = {"Jan", "Anna", "Piotr", "Maria", "Krzysztof", "Katarzyna", "Andrzej", "Magdalena", "Tomasz", "Agnieszka"};
    private static final String[] LAST_NAMES = {"Kowalski", "Nowak", "Wisniewski", "Wojcik", "Kowalczyk", "Kaminski", "Lewandowski", "Zielinski", "Szymanski", "Wozniak"};

    private static final HttpProtocolBuilder httpProtocol = http.baseUrl(baseUrl)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .shareConnections();

    private static final ScenarioBuilder scenario = scenario("C3 Create Customer")
            .exec(session -> {
                // Generate random customer data
                String uniqueId = UUID.randomUUID().toString().substring(0, 8);
                String email = "warmup-" + uniqueId + "@test.com";
                String passwordHash = "$2a$10$" + UUID.randomUUID().toString().replace("-", "").substring(0, 22); // BCrypt salt = 22 chars
                String firstName = FIRST_NAMES[ThreadLocalRandom.current().nextInt(FIRST_NAMES.length)];
                String lastName = LAST_NAMES[ThreadLocalRandom.current().nextInt(LAST_NAMES.length)];
                String phone = "+48" + ThreadLocalRandom.current().nextInt(500000000, 600000000);
                
                return session
                    .set("email", email)
                    .set("passwordHash", passwordHash)
                    .set("firstName", firstName)
                    .set("lastName", lastName)
                    .set("phone", phone);
            })
            .exec(
                    http("POST /api/shop/customers")
                            .post(endpoint)
                            .body(StringBody("{\"email\":\"#{email}\",\"passwordHash\":\"#{passwordHash}\",\"firstName\":\"#{firstName}\",\"lastName\":\"#{lastName}\",\"phone\":\"#{phone}\"}"))
                            .check(status().is(200)));

    {
        setUp(scenario.injectOpen(rampUsers(10).during(10)))
                .protocols(httpProtocol);
    }
}
