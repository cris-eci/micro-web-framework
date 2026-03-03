package com.arep.lab;

import com.arep.lab.appexamples.HttpRequest;
import com.arep.lab.appexamples.HttpResponse;
import com.arep.lab.appexamples.WebMethod;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Micro-web-framework HTTP server.
 *
 * <p>Usage:
 * <pre>
 *   staticfiles("/webroot/public");
 *   get("/hello", (req, res) -> "Hello " + req.getValues("name"));
 *   HttpServer.start();
 * </pre>
 *
 * <p>REST endpoints are accessible under the {@code /App} prefix:
 *   {@code http://localhost:8080/App/hello?name=World}
 * <p>Static files are served from the configured classpath directory:
 *   {@code http://localhost:8080/index.html}
 */
public class HttpServer {

    /** Default port the server listens on. */
    public static final int PORT = 8080;

    /** URL path prefix for all REST endpoints. */
    private static final String REST_PREFIX = "/App";

    /** Registered REST route handlers. */
    private static final Map<String, WebMethod> endpoints = new HashMap<>();

    /** Classpath-relative directory for static files (e.g. "/webroot/public"). */
    private static String staticFilesLocation = "/public";

    /** Reference to the active server socket – allows graceful shutdown via stop(). */
    private static ServerSocket activeServerSocket;

    // -------------------------------------------------------------------------
    // Framework public API
    // -------------------------------------------------------------------------

    /**
     * Registers a GET handler for the given route path.
     *
     * @param path route path (e.g. "/hello")
     * @param wm   lambda that receives request/response and returns the body string
     */
    public static void get(String path, WebMethod wm) {
        endpoints.put(path, wm);
    }

    /**
     * Configures the classpath root directory for static files.
     *
     * @param location classpath-relative directory (e.g. "/webroot/public")
     */
    public static void staticfiles(String location) {
        staticFilesLocation = location;
    }

    /**
     * Starts the HTTP server on the default port ({@value PORT}) and blocks
     * indefinitely, handling one connection per iteration.
     */
    public static void start() throws IOException {
        start(PORT);
    }

    /**
     * Starts the HTTP server on the specified port.
     *
     * @param port TCP port to listen on
     */
    public static void start(int port) throws IOException {
        activeServerSocket = new ServerSocket(port);
        System.out.println("Server started on http://localhost:" + port);
        try (ServerSocket serverSocket = activeServerSocket) {
            while (!serverSocket.isClosed()) {
                try (Socket clientSocket = serverSocket.accept()) {
                    handleConnection(clientSocket);
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        System.err.println("Error handling connection: " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Stops the running server by closing the underlying socket.
     */
    public static void stop() throws IOException {
        if (activeServerSocket != null && !activeServerSocket.isClosed()) {
            activeServerSocket.close();
        }
    }

    /**
     * Clears all registered routes (useful for resetting state between tests).
     */
    public static void clearRoutes() {
        endpoints.clear();
    }

    // -------------------------------------------------------------------------
    // Request / Response handling
    // -------------------------------------------------------------------------

    private static void handleConnection(Socket clientSocket) throws IOException {
        OutputStream rawOut = clientSocket.getOutputStream();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));

        // --- Parse request line --------------------------------------------------
        String requestLine = in.readLine();
        if (requestLine == null || requestLine.isBlank()) return;

        System.out.println("[Request] " + requestLine);

        String[] tokens = requestLine.split(" ");
        if (tokens.length < 2) return;

        String httpMethod = tokens[0];
        String rawUri     = tokens[1];

        // Consume remaining headers (ignore body for now)
        String headerLine;
        while ((headerLine = in.readLine()) != null && !headerLine.isBlank()) {
            // headers consumed but not used beyond connection handling
        }

        // --- Parse URI -----------------------------------------------------------
        URI uri;
        try {
            uri = new URI(rawUri);
        } catch (URISyntaxException e) {
            sendError(rawOut, 400, "Bad Request");
            return;
        }

        String path  = uri.getPath();
        String query = uri.getRawQuery(); // may be null

        // --- Route: REST endpoint -----------------------------------------------
        if (path.startsWith(REST_PREFIX + "/") || path.equals(REST_PREFIX)) {
            String routeKey = path.substring(REST_PREFIX.length()); // strip "/App"
            if (routeKey.isEmpty()) routeKey = "/";

            WebMethod handler = endpoints.get(routeKey);
            if (handler == null) {
                sendError(rawOut, 404, "No endpoint registered for: " + routeKey);
                return;
            }

            HttpRequest  req = new HttpRequest(httpMethod, routeKey, query);
            HttpResponse res = new HttpResponse();

            String body;
            try {
                body = handler.handle(req, res);
            } catch (Exception e) {
                sendError(rawOut, 500, "Handler error: " + e.getMessage());
                return;
            }

            sendTextResponse(rawOut, res.getStatusCode(), res.getContentType(), body);

        // --- Route: Static file -------------------------------------------------
        } else {
            serveStaticFile(rawOut, path);
        }
    }

    // -------------------------------------------------------------------------
    // Static file serving
    // -------------------------------------------------------------------------

    private static void serveStaticFile(OutputStream out, String path) throws IOException {
        // Default to index.html for root requests
        if (path.equals("/") || path.isBlank()) {
            path = "/index.html";
        }

        String resourcePath = staticFilesLocation + path;
        InputStream fileStream = HttpServer.class.getResourceAsStream(resourcePath);

        if (fileStream == null) {
            sendError(out, 404, "File not found: " + path);
            return;
        }

        byte[] fileBytes = fileStream.readAllBytes();
        fileStream.close();

        String contentType = detectContentType(path);

        String header = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "Content-Length: " + fileBytes.length + "\r\n"
                + "\r\n";

        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(fileBytes);
        out.flush();
    }

    // -------------------------------------------------------------------------
    // Response helpers
    // -------------------------------------------------------------------------

    private static void sendTextResponse(OutputStream out, int status,
                                         String contentType, String body) throws IOException {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        String header = "HTTP/1.1 " + status + " " + reasonPhrase(status) + "\r\n"
                + "Content-Type: " + contentType + "; charset=UTF-8\r\n"
                + "Content-Length: " + bodyBytes.length + "\r\n"
                + "\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(bodyBytes);
        out.flush();
    }

    private static void sendError(OutputStream out, int status, String message) throws IOException {
        sendTextResponse(out, status, "text/plain", status + " " + reasonPhrase(status) + ": " + message);
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private static String detectContentType(String path) {
        if (path.endsWith(".html") || path.endsWith(".htm")) return "text/html";
        if (path.endsWith(".css"))  return "text/css";
        if (path.endsWith(".js"))   return "application/javascript";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".png"))  return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".gif"))  return "image/gif";
        if (path.endsWith(".ico"))  return "image/x-icon";
        if (path.endsWith(".svg"))  return "image/svg+xml";
        return "application/octet-stream";
    }

    private static String reasonPhrase(int code) {
        return switch (code) {
            case 200 -> "OK";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            default  -> "";
        };
    }
}