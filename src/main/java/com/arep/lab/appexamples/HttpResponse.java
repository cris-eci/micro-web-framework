package com.arep.lab.appexamples;

/**
 * Represents the HTTP response that will be sent back to the client.
 * Allows the handler to customise the status code and content type.
 */
public class HttpResponse {

    private int statusCode = 200;
    private String contentType = "text/plain";

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /** Convenience method to produce the HTTP status line string. */
    public String getStatusLine() {
        String reason = statusCode == 200 ? "OK"
                : statusCode == 404 ? "Not Found"
                : statusCode == 500 ? "Internal Server Error"
                : "";
        return "HTTP/1.1 " + statusCode + " " + reason;
    }
}
