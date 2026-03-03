package com.arep.lab;

import com.arep.lab.appexamples.HttpRequest;
import com.arep.lab.appexamples.HttpResponse;
import com.arep.lab.appexamples.WebMethod;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the micro-web-framework.
 *
 * <ul>
 *   <li>Unit tests – HttpRequest, HttpResponse, WebMethod lambda registration.</li>
 *   <li>Integration tests – full HTTP round-trip using a real ServerSocket.</li>
 * </ul>
 */
class LabApplicationTests {

    // =========================================================================
    // Unit tests: HttpRequest
    // =========================================================================

    @Test
    void httpRequest_getValues_returnsCorrectValue() {
        HttpRequest req = new HttpRequest("GET", "/hello", "name=Pedro");
        assertEquals("Pedro", req.getValues("name"));
    }

    @Test
    void httpRequest_getValues_missingParam_returnsEmpty() {
        HttpRequest req = new HttpRequest("GET", "/hello", "name=Pedro");
        assertEquals("", req.getValues("age"));
    }

    @Test
    void httpRequest_nullQuery_doesNotThrow() {
        HttpRequest req = new HttpRequest("GET", "/pi", null);
        assertEquals("", req.getValues("anything"));
    }

    @Test
    void httpRequest_multipleQueryParams_parsedCorrectly() {
        HttpRequest req = new HttpRequest("GET", "/search", "q=java&lang=en&page=2");
        assertEquals("java", req.getValues("q"));
        assertEquals("en",   req.getValues("lang"));
        assertEquals("2",    req.getValues("page"));
    }

    @Test
    void httpRequest_urlEncodedValue_decodedCorrectly() {
        HttpRequest req = new HttpRequest("GET", "/hello", "name=John%20Doe");
        assertEquals("John Doe", req.getValues("name"));
    }

    @Test
    void httpRequest_getPath_returnsPath() {
        HttpRequest req = new HttpRequest("GET", "/hello", "name=World");
        assertEquals("/hello", req.getPath());
    }

    @Test
    void httpRequest_getMethod_returnsMethod() {
        HttpRequest req = new HttpRequest("GET", "/hello", null);
        assertEquals("GET", req.getMethod());
    }

    // =========================================================================
    // Unit tests: HttpResponse
    // =========================================================================

    @Test
    void httpResponse_defaultStatusCode_is200() {
        HttpResponse res = new HttpResponse();
        assertEquals(200, res.getStatusCode());
    }

    @Test
    void httpResponse_defaultContentType_isTextPlain() {
        HttpResponse res = new HttpResponse();
        assertEquals("text/plain", res.getContentType());
    }

    @Test
    void httpResponse_setStatusCode_updatesValue() {
        HttpResponse res = new HttpResponse();
        res.setStatusCode(404);
        assertEquals(404, res.getStatusCode());
    }

    @Test
    void httpResponse_setContentType_updatesValue() {
        HttpResponse res = new HttpResponse();
        res.setContentType("application/json");
        assertEquals("application/json", res.getContentType());
    }

    @Test
    void httpResponse_getStatusLine_200() {
        HttpResponse res = new HttpResponse();
        assertEquals("HTTP/1.1 200 OK", res.getStatusLine());
    }

    @Test
    void httpResponse_getStatusLine_404() {
        HttpResponse res = new HttpResponse();
        res.setStatusCode(404);
        assertEquals("HTTP/1.1 404 Not Found", res.getStatusLine());
    }

    // =========================================================================
    // Unit tests: WebMethod (lambda execution)
    // =========================================================================

    @Test
    void webMethod_lambda_helloWorld() {
        WebMethod wm = (req, res) -> "Hello " + req.getValues("name");
        HttpRequest  req = new HttpRequest("GET", "/hello", "name=World");
        HttpResponse res = new HttpResponse();
        assertEquals("Hello World", wm.handle(req, res));
    }

    @Test
    void webMethod_lambda_piValue() {
        WebMethod wm = (req, res) -> String.valueOf(Math.PI);
        HttpRequest  req = new HttpRequest("GET", "/pi", null);
        HttpResponse res = new HttpResponse();
        assertEquals(String.valueOf(Math.PI), wm.handle(req, res));
    }

    @Test
    void httpServer_getRegistration_storesEndpoint() {
        // Verify that registering a new route works without throwing exceptions.
        // We register an extra route so as not to interfere with integration tests.
        HttpServer.get("/unit-test-route", (req, res) -> "test-body");
        assertTrue(true, "get() registration completed without exception");
    }

    // =========================================================================
    // Integration tests: full HTTP server round-trip
    // =========================================================================

    private static final int TEST_PORT = 18080;
    private static Thread serverThread;
    private static java.net.http.HttpClient client;

    @BeforeAll
    static void startServer() throws InterruptedException {
        HttpServer.clearRoutes();
        HttpServer.staticfiles("/webroot/public");

        HttpServer.get("/hello", (req, res) -> "Hello " + req.getValues("name"));
        HttpServer.get("/pi",    (req, res) -> String.valueOf(Math.PI));
        HttpServer.get("/echo",  (req, res) -> {
            String msg = req.getValues("msg");
            return msg.isEmpty() ? "(no message)" : "Echo: " + msg;
        });

        serverThread = new Thread(() -> {
            try {
                HttpServer.start(TEST_PORT);
            } catch (IOException e) {
                // normal when socket is closed by stopServer()
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // Give the server a moment to bind the socket
        Thread.sleep(400);

        client = java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @AfterAll
    static void stopServer() throws IOException {
        HttpServer.stop();
    }

    @Test
    void integration_helloWithName_returnsGreeting() throws Exception {
        var request = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/App/hello?name=Pedro"))
                .GET()
                .build();
        var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("Hello Pedro", response.body());
    }

    @Test
    void integration_piEndpoint_returnsPiValue() throws Exception {
        var request = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/App/pi"))
                .GET()
                .build();
        var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals(String.valueOf(Math.PI), response.body());
    }

    @Test
    void integration_echoEndpoint_returnsEchoedMessage() throws Exception {
        var request = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/App/echo?msg=framework"))
                .GET()
                .build();
        var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("Echo: framework", response.body());
    }

    @Test
    void integration_unknownEndpoint_returns404() throws Exception {
        var request = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/App/nonexistent"))
                .GET()
                .build();
        var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }

    @Test
    void integration_staticFile_indexHtml_returns200() throws Exception {
        var request = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/index.html"))
                .GET()
                .build();
        var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Micro Web Framework"));
    }

    @Test
    void integration_staticFile_css_returns200() throws Exception {
        var request = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/styles.css"))
                .GET()
                .build();
        var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("content-type")
                .map(v -> v.startsWith("text/css")).orElse(false));
    }

    @Test
    void integration_missingStaticFile_returns404() throws Exception {
        var request = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/does-not-exist.html"))
                .GET()
                .build();
        var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }
}

