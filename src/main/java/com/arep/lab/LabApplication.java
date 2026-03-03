package com.arep.lab;

import com.arep.lab.appexamples.MainServices;

/**
 * Main entry point for the micro-web-framework application.
 * Delegates to {@link MainServices} which registers all routes and starts the server.
 */
public class LabApplication {

    public static void main(String[] args) throws Exception {
        MainServices.main(args);
    }
}
