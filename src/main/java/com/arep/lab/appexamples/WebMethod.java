package com.arep.lab.appexamples;

/**
 * Functional interface representing a REST endpoint handler.
 * Implementations receive the parsed request and a response object,
 * and return the body string to send back to the client.
 */
@FunctionalInterface
public interface WebMethod {
    String handle(HttpRequest req, HttpResponse res);
}
