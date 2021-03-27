package com.ohjelmointi3.chatserver;

import java.security.SecureRandom;
import java.sql.ResultSet;
import java.util.Base64;

import com.sun.net.httpserver.BasicAuthenticator;

import org.apache.commons.codec.digest.Crypt;

public class ChatAuthenticator extends BasicAuthenticator {
    
    SecureRandom secureRandom = new SecureRandom();

    public ChatAuthenticator(){
        super("chat");
    }


    @Override
    public boolean checkCredentials(String givenUsername, String givenPassword) {
        String getPasswordQuery = "SELECT password FROM users WHERE username = '" +
                                        givenUsername + "'";

        ResultSet passwordResultSet = ChatDatabase.getInstance().makeQuery(getPasswordQuery);
        try {
            if(passwordResultSet.next()){
                String hashedGivenPassword = Crypt.crypt(givenPassword, passwordResultSet.getString("password"));

                if (passwordResultSet.getString("password").equals(hashedGivenPassword)){
                    passwordResultSet.close();
                    return true;
                }
            }
        } catch (Exception e) {
            ChatServer.log("***ERROR in ChatAuthenticator*** result empty not null: " + e);
        }
        return false;
    }

    public boolean addUser(String username, String password, String email){

        byte bytes[] = new byte[13];
        secureRandom.nextBytes(bytes);
        String salt = new String(Base64.getEncoder().encode(bytes));
        salt = "$6$" + salt;
        String hashedPassword = Crypt.crypt(password, salt);

        String addUserStatement = "INSERT INTO users " +
                                  "VALUES ('" +
                                  username + "', '" +
                                  hashedPassword + "', '" +
                                  salt + "', '" +
                                  email + "')";

        if (ChatDatabase.getInstance().makeStatement(addUserStatement)){
            return true;
        }
        return false;
    }
}
