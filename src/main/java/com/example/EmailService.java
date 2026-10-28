package com.example;

//No implementation exists - and we wouldn't want unit tests sending real emails anyway!
public interface EmailService {

    void send(String to, String subject, String body);
}
