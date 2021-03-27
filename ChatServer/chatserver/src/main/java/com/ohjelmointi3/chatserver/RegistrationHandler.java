package com.ohjelmointi3.chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONObject;

public class RegistrationHandler implements HttpHandler {

    ChatAuthenticator auth = null;

    RegistrationHandler(ChatAuthenticator authenticator) {
        auth = authenticator;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        try {
            if ("POST".equals(httpExchange.getRequestMethod())) {
                handlePost(httpExchange);

            } else {
                sendMessage(httpExchange, 400, "Not supported");
            }

        } catch (Exception e) {
            sendMessage(httpExchange, 500, "Internal server error");
            System.out.println(e);
        }
    }

    private void handlePost(HttpExchange httpExchange) throws IOException {
        if (checkHeaders(httpExchange)) {
            InputStream inputStream = httpExchange.getRequestBody();
            String text = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
                    .collect(Collectors.joining("\n"));
            inputStream.close();
                try {
                    JSONObject credentials = new JSONObject(text);
                    if (checkpost(httpExchange, credentials)) {
                        if(auth.addUser(credentials.getString("username"), 
                                        credentials.getString("password"), 
                                        credentials.getString("email"))){

                            httpExchange.sendResponseHeaders(200, -1);
                            ChatServer.log("Added user: " + credentials.getString("username"));
                        } else{
                            sendMessage(httpExchange, 556, "Username taken");
                        }  
                        
                    }
                } catch (Exception e) {
                    ChatServer.log("***ERROR JSON***: " + e);
                    sendMessage(httpExchange, 555, "JSON is wrong");
                }

        }
    }

    private boolean checkHeaders(HttpExchange httpExchange) throws IOException {
        String contentType = "";
        Headers headers = httpExchange.getRequestHeaders();

        if (!headers.containsKey("Content-Length")) {
            sendMessage(httpExchange, 411, "No content-length");
            return false;
        }
        if (headers.containsKey("Content-Type")) {
            contentType = headers.get("Content-Type").get(0);
        } else {
            sendMessage(httpExchange, 400, "No content type in request");
            return false;
        }
        if (!contentType.equalsIgnoreCase("application/json")) {
            sendMessage(httpExchange, 411, "Wrong content type");
            return false;
        }
        return true;
    }

    private boolean checkpost(HttpExchange httpExchange, JSONObject credentials) throws IOException {
        if (credentials.length() == 3) {
            if (credentials.has("username")  && credentials.has("password") && credentials.has("email")) {
                if (credentials.getString("username").trim().length() > 0 && 
                    credentials.getString("password").trim().length() > 0 &&
                    credentials.getString("email").trim().length() > 0) {
                        return true;
                    } else{
                        sendMessage(httpExchange, 400, "JSON object has empty value");
                    }
            } else {
                sendMessage(httpExchange, 400, "JSON object has wrong keys");
            }
        } else {
            sendMessage(httpExchange, 400, "JSON object is wrong lenght");
        }
        
        return false;
    }

    private void sendMessage(HttpExchange httpExchange, int statusCode, String messageBody) throws IOException {
        if (statusCode < 200 || statusCode > 299) {
            ChatServer.log("*** ERROR in /registration Sending message *** : " + statusCode + ", " + messageBody);
        } else {
            ChatServer.log("Sending message: " + statusCode + ", " + messageBody);
        }

        byte[] responseBytes = messageBody.getBytes("UTF-8");
        httpExchange.sendResponseHeaders(statusCode, responseBytes.length);
        OutputStream outputStream = httpExchange.getResponseBody();
        outputStream.write(responseBytes);
        outputStream.close();
    }

}
