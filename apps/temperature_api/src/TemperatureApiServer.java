import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;

/**
 * Эндпоинты:
 *   GET /temperature?location=Living%20Room
 *   GET /temperature/{sensorId}
 */
public class TemperatureApiServer {

    private static final Map<String, String> LOCATION_TO_ID = new HashMap<>();
    private static final Map<String, String> ID_TO_LOCATION = new HashMap<>();
    private static final Random RANDOM = new Random();

    static {
        LOCATION_TO_ID.put("Living Room", "1");
        LOCATION_TO_ID.put("Bedroom", "2");
        LOCATION_TO_ID.put("Kitchen", "3");

        ID_TO_LOCATION.put("1", "Living Room");
        ID_TO_LOCATION.put("2", "Bedroom");
        ID_TO_LOCATION.put("3", "Kitchen");
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8081"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/temperature", new TemperatureHandler());
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("Temperature API started on port " + port);
    }

    private static class TemperatureHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();

            String sensorId = null;
            String location = null;

            // sensorId из пути /temperature/{sensorId}
            if (path.length() > "/temperature".length()) {
                sensorId = path.substring("/temperature".length() + 1);
            }

            // location из query ?location=...
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] kv = param.split("=", 2);
                    if (kv.length == 2 && "location".equals(kv[0])) {
                        location = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                    }
                }
            }

            // Если location не задан — выводим по sensorId
            if (location == null || location.isEmpty()) {
                location = ID_TO_LOCATION.getOrDefault(sensorId, "Unknown");
            }

            // Если sensorId не задан — выводим по location
            if (sensorId == null || sensorId.isEmpty()) {
                sensorId = LOCATION_TO_ID.getOrDefault(location, "0");
            }

            double value = 15.0 + RANDOM.nextDouble() * 15.0;
            String json = String.format(
                    Locale.US,
                    "{\"value\":%.2f,\"unit\":\"\u00B0C\",\"timestamp\":\"%s\",\"location\":\"%s\",\"status\":\"active\",\"sensor_id\":\"%s\",\"sensor_type\":\"temperature\",\"description\":\"Random temperature for %s\"}",
                    value,
                    OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    location,
                    sensorId,
                    location
            );

            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        }
    }
}
