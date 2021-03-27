package com.ohjelmointi3.chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.sun.net.httpserver.*;

import org.json.JSONArray;
import org.json.JSONObject;

public class ChatHandler implements HttpHandler {

    private ArrayList<JSONObject> messages = new ArrayList<JSONObject>();

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        try {
            if ("GET".equals(httpExchange.getRequestMethod())) {

                handleGet(httpExchange);

            } else if ("POST".equals(httpExchange.getRequestMethod())) {
                handlePost(httpExchange);

            } else {
                sendMessage(httpExchange, 400, "Not supported");
            }

        } catch (Exception e) {
            sendMessage(httpExchange, 500, "Internal server error: " + e);
            System.out.println(e);
        }
    }

    private void handleGet(HttpExchange httpExchange) throws IOException {
        JSONArray messageArray = new JSONArray();

        ZonedDateTime latestMessageTime = ZonedDateTime.now().minusDays(1);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

        if (httpExchange.getRequestHeaders().containsKey("If-Modified-Since")) {
            String modifiedSince = httpExchange.getRequestHeaders().getFirst("If-Modified-Since");
            ZonedDateTime modifiedSinceTime = ZonedDateTime.parse(modifiedSince, dateTimeFormatter);
            if (modifiedSinceTime.compareTo(latestMessageTime) < 1) {
                latestMessageTime = modifiedSinceTime;
            }
        }

        long latestMessageEpoch = latestMessageTime.toInstant().toEpochMilli();
        String getMessagesQuery = "SELECT * FROM messages where sent > " + latestMessageEpoch + " order by sent asc";
        ResultSet messagesResultSet = ChatDatabase.getInstance().makeQuery(getMessagesQuery);

        try {
            while (messagesResultSet.next()) {
                ZonedDateTime messageTime = ZonedDateTime
                        .ofInstant(Instant.ofEpochMilli(messagesResultSet.getLong("sent")), ZoneOffset.UTC);
                String messageTimeString = messageTime.format(dateTimeFormatter);

                if (messageTime.compareTo(latestMessageTime) < 1) {
                    latestMessageTime = messageTime;
                }

                JSONObject jsonMessage = new JSONObject("{\"message\":\"" + messagesResultSet.getString("message") + "\"," + 
                                                        "\"user\":\"" + messagesResultSet.getString("user") + "\"," + 
                                                        "\"sent\":\"" + messageTimeString + "\"}");

                messageArray.put(jsonMessage);
            }
        } catch (Exception e) {
            ChatServer.log("***ERROR JSON***: " + e);
        }

        String latestMessageTimeString = latestMessageTime.format(dateTimeFormatter);
        httpExchange.getResponseHeaders().add("Last-Modified", latestMessageTimeString);

        if (!messageArray.isEmpty()) {
            String messageBody = messageArray.toString();
            sendMessage(httpExchange, 200, messageBody);
        } else {
            ChatServer.log("No new messages to deliver to client");
            httpExchange.sendResponseHeaders(204, -1);
        }
    }

    private void sendMessage(HttpExchange httpExchange, int statusCode, String messageBody) throws IOException {
        if (statusCode < 200 || statusCode > 299) {
            ChatServer.log("*** ERROR in /chat Sending message *** : " + statusCode + ", " + messageBody);
        } else {
            ChatServer.log("Sending message: " + statusCode + ", " + messageBody.length());
        }

        byte[] responseBytes = messageBody.getBytes("UTF-8");
        httpExchange.sendResponseHeaders(statusCode, responseBytes.length);
        OutputStream outputStream = httpExchange.getResponseBody();
        outputStream.write(responseBytes);
        outputStream.close();
    }

    private void handlePost(HttpExchange httpExchange) throws IOException {
        if (checkHeaders(httpExchange)) {
            InputStream inputStream = httpExchange.getRequestBody();
            String messageText = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
                    .collect(Collectors.joining("\n"));
            inputStream.close();
            try {
                JSONObject message = new JSONObject(messageText);
                if (checkPost(httpExchange, message)) {
                    httpExchange.sendResponseHeaders(200, -1);
                    processMessage(message);
                }
            } catch (Exception e) {
                sendMessage(httpExchange, 555, "JSON is wrong");
            }
        }
    }

    private boolean checkHeaders(HttpExchange httpExchange) throws IOException {
        Headers headers = httpExchange.getRequestHeaders();
        String contentType;

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
            sendMessage(httpExchange, 411, "No content type in request");
            return false;
        }
        return true;
    }

    private boolean checkPost(HttpExchange httpExchange, JSONObject message) throws IOException {
        if (message.length() == 3) {
            if (message.has("user") && message.has("message") && message.has("sent")) {
                return true;
            } else {
                sendMessage(httpExchange, 400, "JSON object has wrong keys");
            }
        } else {
            sendMessage(httpExchange, 400, "JSON object is wrong lenght");
        }
        return false;
    }

    private void processMessage(JSONObject message) {
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(message.getString("sent"));

        String addMessageUpdate = "INSERT INTO messages " + "VALUES ('" + zonedDateTime.toInstant().toEpochMilli()
                + "', '" + message.getString("user") + "', '" + message.getString("message") + "')";

        if (ChatDatabase.getInstance().makeStatement(addMessageUpdate)) {
            ChatServer.log("Adding message: " + addMessageUpdate.length());
        } else {
            ChatServer.log("***ERROR in ChatHandler*** :Adding message failed: " + addMessageUpdate);
        }

    }

}
