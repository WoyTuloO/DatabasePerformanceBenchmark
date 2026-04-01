package example;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * R2 – Dostępne produkty marki w mieście
 * Warmup simulation: 10 requests
 * Generuje losowe brandName i city z predefiniowanych list
 */
public class R2AvailableProductsSimulation extends Simulation {

    private static final String baseUrl = System.getProperty("baseUrl", "http://localhost:8080");
    
    private static final String[] BRAND_NAMES = {"Nike", "Adidas", "Puma", "Samsung", "Apple", "Sony", "LG", "Dell", "HP", "Lenovo"};
    private static final String[] CITIES = {"Warsaw", "Krakow", "Wroclaw", "Poznan", "Gdansk", "Szczecin", "Lodz", "Bydgoszcz", "Lublin", "Katowice"};

    private static final HttpProtocolBuilder httpProtocol = http.baseUrl(baseUrl)
            .acceptHeader("application/json")
            .shareConnections();

    private static final ScenarioBuilder scenario = scenario("R2 Available Products")
            .exec(session -> {
                String brandName = BRAND_NAMES[ThreadLocalRandom.current().nextInt(BRAND_NAMES.length)];
                String city = CITIES[ThreadLocalRandom.current().nextInt(CITIES.length)];
                return session.set("brandName", brandName).set("city", city);
            })
            .exec(
                    http("GET /api/shop/products/available")
                            .get("/api/shop/products/available?brandName=#{brandName}&city=#{city}")
                            .check(status().in(200, 404)));

    {
        setUp(scenario.injectOpen(rampUsers(10).during(10)))
                .protocols(httpProtocol)
                .assertions(
                        global().responseTime().mean().lt(1000));
    }
}

