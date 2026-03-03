package com.arep.lab.appexamples;

import com.arep.lab.HttpServer;

import static com.arep.lab.HttpServer.get;
import static com.arep.lab.HttpServer.staticfiles;

/**
 * Demo application that shows how to use the micro-web-framework.
 *
 * <p>Start the server and try:
 * <ul>
 *   <li>Static file : http://localhost:8080/index.html</li>
 *   <li>REST hello  : http://localhost:8080/App/hello?name=Pedro</li>
 *   <li>REST pi     : http://localhost:8080/App/pi</li>
 * </ul>
 */
public class MainServices {

    public static void main(String[] args) throws Exception {

        // 1. Configure static files location (served from classpath)
        staticfiles("/webroot/public");

        // 2. Register REST endpoints using lambda expressions
        get("/hello", (req, res) -> "Hello " + req.getValues("name"));

        get("/pi", (req, res) -> String.valueOf(Math.PI));

        get("/echo", (req, res) -> {
            String msg = req.getValues("msg");
            return msg.isEmpty() ? "(no message)" : "Echo: " + msg;
        });

        // 3. Start the server (blocks)
        HttpServer.start();
    }
}
