package com.arep.lab;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

import com.arep.lab.appexamples.WebMethod;

import java.io.*;
 
public class HttpServer {

    static Map<String, WebMethod> endpoints = new HashMap<>();
 
    public static void main(String[] args) throws IOException, URISyntaxException {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(35000);
        } catch (IOException e) {
            System.err.println("Could not listen on port: 35000.");
            System.exit(1);
        }
        Socket clientSocket = null;
        boolean running = true;
 
        while (running) {
            try {
                System.out.println("Listo para recibir ...");
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                System.err.println("Accept failed.");
                System.exit(1);
            }
 
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            clientSocket.getInputStream()));
            String inputLine, outputLine;
 
            boolean isFirstLine = true;
            String reqpath = "";
 
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Received: " + inputLine);
 
                if (isFirstLine) {
                    String[] flTokens = inputLine.split(" ");
                    String method = flTokens[0];
                    String struripath = flTokens[1];
                    String protocolversion = flTokens[2];
 
                    URI uripath = new URI(struripath);
                    reqpath = uripath.getPath();
 
                    System.out.println("Path: " + reqpath);
 
                    isFirstLine = false;
                }
 
                if (!in.ready()) {
                    break;
                }
            }

            WebMethod currentWm = endpoints.get(reqpath);
 
            if (currentWm != null){
                outputLine
                        = "HTTP/1.1 200 OK\n\r"
                        + "Content-Type: text/html\n\r"
                        + "\n\r"
                        + "<!DOCTYPE html>"
                        + "<html>"
                        + "<head>"
                        + "<meta charset=\"UTF-8\">"
                        + "<title>Backend Service</title>\n"
                        + "</head>"
                        + "<body>"
                        + "PI= " + Math.PI
                        + "</body>"
                        + "</html>";
 
            } else {
                outputLine
                        = "HTTP/1.1 200 OK\n\r"
                        + "Content-Type: text/html\n\r"
                        + "\n\r"
                        + "<!DOCTYPE html>"
                        + "<html>"
                        + "<head>"
                        + "<meta charset=\"UTF-8\">"
                        + "<title>Title of the document</title>\n"
                        + "</head>"
                        + "<body>"
                        + "My Web Site"
                        + "</body>"
                        + "</html>";
            }
            out.println(outputLine);
 
            out.close();
            in.close();
            clientSocket.close();
        }
        serverSocket.close();
    }

    public static void get(String path, WebMethod wm) {
        endpoints.put(path, wm);
    };

}