package com.ohjelmointi3.chatserver;

public class User {
    String username;
    String password;
    String email;
    
    public User(String user, String pass, String mail){
        username = user;
        password = pass;
        email = mail;
    }
}
