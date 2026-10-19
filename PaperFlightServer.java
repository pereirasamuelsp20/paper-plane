import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Small local HTTP bridge that connects the Paper Flight browser game to Java.
 * The browser asks this server to calculate its survival-only score. All other
 * real-time rendering and input remain in the browser.
 */
public final class PaperFlightServer {
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8787"));
    private static final Path ROOT = Path.of(".").toAbsolutePath().normalize();

    private PaperFlightServer() { }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/status", PaperFlightServer::handleStatus);
        server.createContext("/api/score", PaperFlightServer::handleScore);
        server.createContext("/", PaperFlightServer::handleStaticFile);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        System.out.println("Paper Flight Java bridge is running.");
        System.out.println("Listening on port " + PORT + " on all network interfaces.");
        System.out.println("For local testing, open http://localhost:" + PORT + " in your browser.");
        System.out.println("Java is calculating the live survival score at /api/score.");
        System.out.println("Press Ctrl+C to stop the server.");
    }

    private static void handleStatus(HttpExchange exchange) throws IOException {
        if (!isGet(exchange)) return;
        sendJson(exchange, 200, "{\"connected\":true,\"engine\":\"Java\",\"scorePerSecond\":"
                + PaperFlightEngine.SCORE_PER_SECOND + "}");
    }

    private static void handleScore(HttpExchange exchange) throws IOException {
        if (!isGet(exchange)) return;
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        String rawElapsed = query.get("elapsed");
        if (rawElapsed == null) {
            sendJson(exchange, 400, "{\"error\":\"Missing elapsed query parameter\"}");
            return;
        }

        final double elapsed;
        try {
            elapsed = Double.parseDouble(rawElapsed);
        } catch (NumberFormatException ex) {
            sendJson(exchange, 400, "{\"error\":\"Elapsed must be a number\"}");
            return;
        }
        if (!Double.isFinite(elapsed) || elapsed < 0.0 || elapsed > 86_400.0) {
            sendJson(exchange, 400, "{\"error\":\"Elapsed is outside the accepted range\"}");
            return;
        }

        // This is the real Java scoring function used by PaperFlightEngine too.
        int score = PaperFlightEngine.scoreForElapsed(elapsed);
        String body = "{\"engine\":\"Java\",\"elapsed\":" + elapsed
                + ",\"score\":" + score + "}";
        sendJson(exchange, 200, body);
    }

    private static boolean isGet(HttpExchange exchange) throws IOException {
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) return true;
        sendJson(exchange, 405, "{\"error\":\"GET only\"}");
        return false;
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> params = new HashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) return params;
        for (String part : rawQuery.split("&")) {
            int split = part.indexOf('=');
            if (split < 0) continue;
            String key = URLDecoder.decode(part.substring(0, split), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(part.substring(split + 1), StandardCharsets.UTF_8);
            params.put(key, value);
        }
        return params;
    }

    private static void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static void handleStaticFile(HttpExchange exchange) throws IOException {
        if (!isGet(exchange)) return;
        String requestPath = exchange.getRequestURI().getPath();
        if (requestPath.equals("/")) requestPath = "/paper-plane-gust-challenge.html";

        // Explicit allow-list avoids accidentally exposing arbitrary local files.
        String filename = switch (requestPath) {
            case "/paper-plane-gust-challenge.html" -> "paper-plane-gust-challenge.html";
            case "/PaperFlightEngine.java" -> "PaperFlightEngine.java";
            case "/PaperFlightServer.java" -> "PaperFlightServer.java";
            case "/README-Paper-Flight.txt" -> "README-Paper-Flight.txt";
            case "/Paper-Flight-Presentation.zip" -> "Paper-Flight-Presentation.zip";
            default -> null;
        };
        if (filename == null) {
            sendText(exchange, 404, "Not found");
            return;
        }

        Path file = ROOT.resolve(filename).normalize();
        if (!file.startsWith(ROOT) || !Files.isRegularFile(file)) {
            sendText(exchange, 404, "File not found. Keep the project files in the same folder as the server.");
            return;
        }

        String contentType = filename.endsWith(".html") ? "text/html; charset=utf-8"
                : filename.endsWith(".java") || filename.endsWith(".txt") ? "text/plain; charset=utf-8"
                : "application/zip";
        byte[] bytes = Files.readAllBytes(file);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        if (filename.endsWith(".zip")) {
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"Paper-Flight-Presentation.zip\"");
        }
        exchange.sendResponseHeaders(200, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static void sendText(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
