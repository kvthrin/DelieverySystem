// Kathrin Wilms, 232436

package com.example;

import akka.actor.typed.ActorRef;

public class Paket {

    private String content;
    private String sender;
    private ActorRef<Customer.Message> receiver;

    Paket(String c, String s, ActorRef<Customer.Message> r){
        content = c;
        sender = s;
        receiver= r;
    }

    public String getContent() {
        return content;
    }
    public String getSender() {
        return sender;
    }
    public ActorRef<Customer.Message> getReceiver() {
        return receiver;
    }
}
