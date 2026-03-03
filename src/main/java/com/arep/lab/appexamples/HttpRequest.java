package com.arep.lab.appexamples;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an incoming HTTP request.
 * Provides access to the request path, HTTP method, and parsed query parameters.
 */
public class HttpRequest {

    private final String method;
    private final String path;
    private final Map<String, String> queryParams;

    public HttpRequest(String method, String path, String queryString) {
        this.method = method;
        this.path = path;
        this.queryParams = new HashMap<>();
        if (queryString != null && !queryString.isEmpty()) {
            parseQueryString(queryString);
        }
    }

    /**
     * Parses key=value pairs from the raw query string.
     * Handles URL-encoded characters and multiple parameters separated by '&'.
     */
    private void parseQueryString(String queryString) {
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
            String value = keyValue.length == 2
                    ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8)
                    : "";
            queryParams.put(key, value);
        }
    }

    /**
     * Returns the value of a query parameter by name, or an empty string if absent.
     *
     * @param param the parameter name
     * @return the parameter value
     */
    public String getValues(String param) {
        return queryParams.getOrDefault(param, "");
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getQueryParams() {
        return Collections.unmodifiableMap(queryParams);
    }
}
