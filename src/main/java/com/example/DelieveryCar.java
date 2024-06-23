// Kathrin Wilms, 232436

package com.example;

import akka.actor.typed.javadsl.TimerScheduler;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.time.Duration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class DelieveryCar  extends AbstractBehavior<DelieveryCar.Message> {

    public interface Message {
    }

    public record Load(List<Paket> pakets) implements Message {}
    public record PickupResponse(Optional<Paket> paket) implements Message {}
    public record CheckPackages() implements Message {} // Checks whether the car has a package for a customer

    public static Behavior<DelieveryCar.Message> create(List<ActorRef<Customer.Message>> route, ActorRef<DistributionCenter.Message> dist) {
        return Behaviors.setup(context -> Behaviors.withTimers(timers -> new DelieveryCar(context, timers, route, dist)));
    }

    private List<ActorRef<Customer.Message>> actorRefList; // Route with Actor references
    private final TimerScheduler timers;
    private List<Paket> paketList;
    private int currentPos;
    private ActorRef<DistributionCenter.Message> distributionCenterRef; // Reference for sending Arrive Message


    public DelieveryCar(ActorContext<DelieveryCar.Message> context, TimerScheduler timers, List<ActorRef<Customer.Message>> route, ActorRef<DistributionCenter.Message> dist) {
        super(context);
        actorRefList = route;
        this.timers = timers;
        this.distributionCenterRef = dist;
    }

    @Override
    public Receive<DelieveryCar.Message> createReceive() {
        return newReceiveBuilder()
                .onMessage(Load.class, this::onLoad)
                .onMessage(CheckPackages.class, this::onCheckPackages)
                .onMessage(PickupResponse.class, this::onPickupResponse)
                .build();
    }

    private Behavior<DelieveryCar.Message> onLoad(Load msg) {
        paketList = new ArrayList<>(msg.pakets);
        currentPos = -1; //Resetting Route-Position
        this.timers.startSingleTimer(new CheckPackages(), Duration.ofSeconds(3));

        return this;

    }

    private Behavior<DelieveryCar.Message> onCheckPackages(CheckPackages msg) {
        if (currentPos < actorRefList.size() - 1) { // Car is not at the last customer
            ActorRef<Customer.Message> cust = actorRefList.get(++currentPos);
            getContext().getLog().info("{} is visiting its {}th customer {}", this.getContext().getSelf().path().name(), this.currentPos+1, cust.path().name());

            Iterator<Paket> iterator = paketList.iterator();
            boolean hasPaket = false;
            while (iterator.hasNext()) { //Searching for fitting paket
                Paket paket = iterator.next();
                if (paket.getReceiver().equals(cust)) {
                    getContext().getLog().info("{} has a paket for {} with {} inside!", this.getContext().getSelf().path().name(),cust.path().name(), paket.getContent());
                    cust.tell(new Customer.Delivery(paket));
                    iterator.remove();
                    hasPaket = true;
                }
            }
            if(!hasPaket){
                getContext().getLog().info("{} is has no paket for {}", this.getContext().getSelf().path().name(), cust.path().name());
            }
            if (paketList.size() < 3) { // Checking whether theres place in the cars storage
                getContext().getLog().info("{} asks {} if they want to hand over a paket", this.getContext().getSelf().path().name(), cust.path().name());
                cust.tell(new Customer.Pickup(getContext().getSelf()));
            } else {
                getContext().getLog().info("{} has no place for more pakets and is moving on to the next stop!", this.getContext().getSelf().path().name());
                this.timers.startSingleTimer(new CheckPackages(), Duration.ofSeconds(1));
            }
        } else { // Car finished its route
            getContext().getLog().info("{} finished its route and is driving back to the distribution center", this.getContext().getSelf().path().name());
            distributionCenterRef.tell(new DistributionCenter.Arrive(getContext().getSelf(), new ArrayList<>(paketList)));
        }
        return this;
    }


    private Behavior<DelieveryCar.Message> onPickupResponse(PickupResponse msg){
       if(msg.paket.isPresent()){
           paketList.add(msg.paket.get());
           StringBuilder paketContents = new StringBuilder(); // Creates list of the cars pakets to log them
           for (int i = 0; i < paketList.size(); i++) {
               if (i > 0) paketContents.append(", ");
               paketContents.append(paketList.get(i).getContent());
           }
           getContext().getLog().info("There are {} pakets in {}'s storage, containing following things: {}",
                   paketList.size(),
                   this.getContext().getSelf().path().name(),
                   paketContents);
       }
        this.timers.startSingleTimer(new CheckPackages(), Duration.ofSeconds(1)); // Checking next stop
       return this;
    }
}
