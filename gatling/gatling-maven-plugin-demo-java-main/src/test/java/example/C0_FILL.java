package example;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class C0_FILL extends Simulation {

    private static final String baseUrl = System.getProperty("baseUrl", "http://localhost:8080");
    private static final String endpoint = "/actuator/health";

    private static final HttpProtocolBuilder httpProtocol = http.baseUrl(baseUrl)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .shareConnections();

    private static final ScenarioBuilder scenario = scenario("NULL0 HEALTH")
            .exec(
                    http("POST /actuator/health")
                            .post(endpoint)
                            .check(status().is(200)));

    {
        setUp(scenario.injectOpen(atOnceUsers(1)))
                .protocols(httpProtocol);
    }
}
