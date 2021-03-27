package com.ohjelmointi3.chatserver;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ChatDatabase {

    Connection connection;
    PreparedStatement statement = null;

    private static ChatDatabase singleton = null;

    public static synchronized ChatDatabase getInstance() {
    if (null == singleton) {
        singleton = new ChatDatabase();
    }
    return singleton;
    }

    private ChatDatabase() {
    }

    public void open(String databaseName) throws SQLException{
        boolean databaseExists = false;
        String databaseLocation = "src\\main\\java\\com\\ohjelmointi3\\chatserver\\" + databaseName;
        File databaseFile = new File(databaseLocation);
        if(databaseFile.exists()) databaseExists = true;
        String databaseConnectionAddress = "jdbc:sqlite:" + databaseLocation;
        try {
            connection = DriverManager.getConnection(databaseConnectionAddress);
        } catch (Exception e) {
            ChatServer.log("***ERROR in ChatDatabase*** getConnection failed" + e);
        }

        if(!databaseExists){
            initializeDatabase();
        }

    }

    public void initializeDatabase() throws SQLException{
        ChatServer.log("Creating tables");
        String createUserTableStatement = "CREATE TABLE users ("
                                        + "username text PRIMARY KEY,"
                                        + "password text NOT NULL,"
                                        + "salt text NOT NULL,"
                                        + "email text NOT NULL)";

        String createMessagesTableStatement = "CREATE TABLE messages ("
                                            + "sent int PRIMARY KEY,"
                                            + "user text NOT NULL,"
                                            + "message text NOT NULL)";
                                            //+ "PRIMARY KEY ("sent", "user"))

        makeStatement(createUserTableStatement);
        makeStatement(createMessagesTableStatement);
    }

    public boolean makeStatement(String update){
        try {
            statement = connection.prepareStatement(update);
            statement.executeUpdate();

            return true;
        } catch (Exception e) {
            ChatServer.log("***ERROR in ChatDatabase*** update failed: " + e);
        }
        return false;
    }

    public ResultSet makeQuery(String query){
        ResultSet result = null;
        try {
            statement = connection.prepareStatement(query);
            result = statement.executeQuery();
        } catch (Exception e) {
            ChatServer.log("***ERROR in ChatDatabase*** query failed: " + e);
        }
        return result;
    }
}
