// Kathrin Wilms, 232436

package com.example;


import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.util.Optional;
import java.util.Random;

public class Customer extends AbstractBehavior<Customer.Message>{

    public interface Message{};

    public record Delivery(Paket paket) implements Message {}
    public record Pickup(ActorRef<DelieveryCar.Message> ref) implements Message {}

    public record RequestAnswer(ActorRef<Customer.Message> otherActorRef, ActorRef<DelieveryCar.Message> car) implements Message {} // Answer from the Addressbook when asking for a random customer

    public static Behavior<Message> create(String name, ActorRef<AddressBook.Message> ref) {
        return Behaviors.setup(context -> new Customer(context, name, ref));
    }

    private final String name;
    private final ActorRef<AddressBook.Message> addressBookActorRef;

    private Customer(ActorContext<Message> context, String name, ActorRef<AddressBook.Message> ref) {
        super(context);
        this.name = name;
        this.addressBookActorRef = ref;

    }
    @Override
    public Receive<Customer.Message> createReceive() {
        return newReceiveBuilder()
                .onMessage(Delivery.class, this::onDelivery)
                .onMessage(Pickup.class, this::onPickup)
                .onMessage(RequestAnswer.class, this::onRequestAnswer)
                .build();
    }


    private Behavior<Message> onDelivery(Delivery msg){
        getContext().getLog().info("I ({}) got a paket with: {} , by {}", this.name, msg.paket.getContent(), msg.paket.getSender());
        return this;
    }

    private Behavior<Message> onPickup(Pickup msg){
        if (hasPaket()){
            addressBookActorRef.tell(new AddressBook.Request(getContext().getSelf(), msg.ref)); // Requesting random customer from the addressbook
        }
        else{
            getContext().getLog().info("{} has no paket for {}", this.name, msg.ref.path().name());
           msg.ref.tell(new DelieveryCar.PickupResponse(Optional.empty())); // Sending no paket
        }
        return this;
    }

    private Behavior<Message> onRequestAnswer(RequestAnswer msg){
        Paket paket = new Paket(contentPicker(), this.name, msg.otherActorRef);
        msg.car.tell(new DelieveryCar.PickupResponse(Optional.of(paket))); // Giving paket to delievery car
        getContext().getLog().info("I ({}) gave a paket with: {} to {}", this.name, paket.getContent(), msg.car.path().name());
        return this;
    }

    private Boolean hasPaket(){ // Checking whether customer wants to send a paket back
        Random random = new Random();
        return (random.nextDouble() < 0.8);
    }

    private String contentPicker(){ // Picks a random item for the customer to be giving to the delievery car
        Random random = new Random();
        String[] contents = {"Spülmaschine", "Altes CD-Regal", "Goldbarren", "20kg Hanteln", "Holzkohlegrill", "Blumenerde"};
        return contents[random.nextInt(contents.length)];
    }
}
