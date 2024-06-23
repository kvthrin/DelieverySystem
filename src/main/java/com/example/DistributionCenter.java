// Kathrin Wilms, 232436

package com.example;


import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DistributionCenter extends AbstractBehavior<DistributionCenter.Message> {

    public interface Message {}
    public record Arrive(ActorRef<DelieveryCar.Message> car, List<Paket> pakets) implements Message {}

    public record Start() implements Message {} // Creation of initial delievery cars
    public static Behavior<DistributionCenter.Message> create(List<ActorRef<Customer.Message>> custs) {
        return Behaviors.setup(context -> new DistributionCenter(context, custs));
    }
    private final List<ActorRef<Customer.Message>> customerList; // References to customers for creation routes with permutations of customers
    private List<Paket> storage;

    public DistributionCenter(ActorContext<Message> context, List<ActorRef<Customer.Message>> custs) {
        super(context);
        customerList = custs;
        storage = new ArrayList<>();

    }

    @Override
    public Receive<DistributionCenter.Message> createReceive() {
        return newReceiveBuilder()
                .onMessage(Arrive.class, this::onArrive)
                .onMessage(Start.class, this::onStart )
                .build();
    }

    private Behavior<Message> onStart(Start msg) {
        // Creation of delievery cars
        Collections.shuffle(customerList); // Creating permutations
        var car1 = this.getContext().spawn(DelieveryCar.create(new ArrayList<>(customerList.subList(0,3)), getContext().getSelf()), "DelieveryCar1");
        Collections.shuffle(customerList);
        var car2 = this.getContext().spawn(DelieveryCar.create(new ArrayList<>(customerList.subList(0,3)), getContext().getSelf()), "DelieveryCar2");
        Collections.shuffle(customerList);
        var car3 = this.getContext().spawn(DelieveryCar.create(new ArrayList<>(customerList.subList(0,3)), getContext().getSelf()), "DelieveryCar3");

        //Sending Load Messages
        car1.tell(new DelieveryCar.Load(storage));
        car2.tell(new DelieveryCar.Load(storage));
        car3.tell(new DelieveryCar.Load(storage));

        getContext().getLog().info("{} , {} and {} started their route!", car1.path().name(), car2.path().name(), car3.path().name());
        return this;
    }
    private Behavior<Message> onArrive(Arrive msg) {
        if(!msg.pakets.isEmpty()) { // Checking if delievery car has pakets left
            storage.addAll(msg.pakets);

            StringBuilder storageContents = new StringBuilder(); // Creating string to log storage
            for (int i = 0; i < storage.size(); i++) {
                if (i > 0) storageContents.append(", ");
                storageContents.append(storage.get(i).getContent());
            }

            getContext().getLog().info("{} gave it pakets to the Delievery Center, the Delievery Center now contains: {}", msg.car.path().name(), storageContents);
        }
        if(storage.size() <= 3){ //Checking whether the whole storages content can be given to the car
            if(storage.isEmpty()){
                getContext().getLog().info("The Delievery Center has no pakets to deliever. It still sends {} off", msg.car.path().name());
            }
            else{
                getContext().getLog().info("The Delievery Center is giving all its {} pakets to {} and sends it off", storage.size(), msg.car.path().name());
            }
            msg.car.tell(new DelieveryCar.Load(new ArrayList<>(storage)));
            storage.clear();
        }
        else{
            Collections.shuffle(storage);
            List<Paket> pakets = storage.subList(0,3);
            msg.car.tell(new DelieveryCar.Load(pakets)); // Sending 3 random pakets to the car
            getContext().getLog().info("The Distribution center gave 3 random pakets to {} and is sending it off", msg.car.path().name());
        }
        getContext().getLog().info("There are {} pakets in the distribution centers storage", storage.size());
        return this;
    }
}