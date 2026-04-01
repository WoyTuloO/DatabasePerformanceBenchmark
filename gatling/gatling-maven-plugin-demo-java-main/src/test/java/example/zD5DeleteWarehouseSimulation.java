package example;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

/**
 * D5 – Usuń magazyn
 * Warmup simulation: 10 requests
 */
public class zD5DeleteWarehouseSimulation extends Simulation {

    private static final String baseUrl = System.getProperty("baseUrl", "http://localhost:8080");
    private static final String csvFile = "data/d5_delete_warehouse.csv";

    // CSV format: warehouseId
    private static final FeederBuilder<String> feeder = csv(csvFile);

    private static final HttpProtocolBuilder httpProtocol = http.baseUrl(baseUrl)
            .acceptHeader("application/json")
            .shareConnections();

    private static final ScenarioBuilder scenario = scenario("D5 Delete Warehouse")
            .feed(feeder)
            .exec(
                    http("DELETE /api/shop/warehouses/{warehouseId}")
                            .delete("/api/shop/warehouses/#{warehouseId}")
                            .check(status().is(200)));

    {
        setUp(scenario.injectOpen(atOnceUsers(4)))
                .protocols(httpProtocol);
    }
}

