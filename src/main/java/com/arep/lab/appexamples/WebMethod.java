package com.arep.lab.appexamples;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public interface WebMethod {
    String execute(HttpRequest req, HttpResponse res);
}
