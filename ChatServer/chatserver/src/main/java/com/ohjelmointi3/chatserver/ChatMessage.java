package com.ohjelmointi3.chatserver;

import java.time.LocalDateTime;

public class ChatMessage {
    public LocalDateTime sent;
    public String nick;
    public String message;

    public ChatMessage(LocalDateTime messageSent, String nickname, String messageBody){
        sent = messageSent;
        nick = nickname;
        message = messageBody;
    }
}
