// Kathrin Wilms, 232436

package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AddressBook extends AbstractBehavior<AddressBook.Message> {

    public interface Message{}

    public record Request(ActorRef<Customer.Message> ref, ActorRef<DelieveryCar.Message> car) implements Message {} //Requesting a random Customer
    public record AddCustomer(ActorRef<Customer.Message> ref, String name) implements Message{} // Adds Customer to the Adressbook

    public static Behavior<AddressBook.Message> create() {
        return Behaviors.setup(AddressBook::new);
    }


    private final List<ActorRef<Customer.Message>> customersList;
    public AddressBook(ActorContext<AddressBook.Message> context) {
        super(context);
        this.customersList = new ArrayList<>();

    }
    @Override
    public Receive<AddressBook.Message> createReceive() {
        return newReceiveBuilder()
                .onMessage(Request.class, this::onRequest)
                .onMessage(AddCustomer.class, this::onAddCustomer)
                .build();
    }


    private Behavior<AddressBook.Message> onRequest(Request msg){
        Random random = new Random();
        if (!customersList.isEmpty()) {
            ActorRef<Customer.Message> customer = customersList.get(random.nextInt(customersList.size())); // Random Selection
            msg.ref.tell(new Customer.RequestAnswer(customer, msg.car)); // Sends selected Customer and itself (for better logging)
        }
        return this;
    }

    private Behavior<AddressBook.Message> onAddCustomer(AddCustomer msg){
        customersList.add(msg.ref);
        getContext().getLog().info("{} added to Addressbook", msg.name);
        return this;
    }

}