package com.arep.lab;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class URLParser {
    public static void main(String[] args) throws MalformedURLException, URISyntaxException {
        URL myurl = new URI("https://ldbn.is.esculaing.edu.co:7865/arep/respuestasexamen.txt?val=35t=4#examenfinal").toURL();

        System.out.println("Protocol: " + myurl.getProtocol());
        System.out.println("Host: " + myurl.getHost());
        System.out.println("Port: " + myurl.getPort());
        System.out.println("Path: " + myurl.getPath());
        System.out.println("Query: " + myurl.getQuery());
        System.out.println("File: " + myurl.getFile());
        System.out.println("Authority: " + myurl.getAuthority());
        System.out.println("Ref: " + myurl.getRef());        
    }
}
