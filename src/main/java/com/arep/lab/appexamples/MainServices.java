package com.arep.lab.appexamples;

import static com.arep.lab.HttpServer.get;

import com.arep.lab.HttpServer;
public class MainServices {
    get("pi", (req, res) -> "PI: " + Math.PI);  
    get("Hello", (req, res) -> "Hello World");
    HttpServer.main(args);

}
