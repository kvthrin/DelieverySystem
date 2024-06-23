// Kathrin Wilms, 232436

package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.util.ArrayList;
import java.util.List;

public class AkkaMainSystem extends AbstractBehavior<AkkaMainSystem.Create> {

    public static class Create {
    }

    public static Behavior<Create> create() {
        return Behaviors.setup(AkkaMainSystem::new);
    }

    private AkkaMainSystem(ActorContext<Create> context) {
        super(context);
    }

    @Override
    public Receive<Create> createReceive() {
        return newReceiveBuilder().onMessage(Create.class, this::onCreate).build();
    }

    private Behavior<Create> onCreate(Create command) {
        //#create-actors

        //Creation of AddressBook
        ActorRef<AddressBook.Message> addressBook = this.getContext().spawn(AddressBook.create(), "addressBook");

        //Creation of Customers
        ActorRef<Customer.Message> alice = this.getContext().spawn(Customer.create("Alice", addressBook), "Alice");
        ActorRef<Customer.Message> bob = this.getContext().spawn(Customer.create("Bob", addressBook), "Bob");
        ActorRef<Customer.Message> charlie = this.getContext().spawn(Customer.create("Charlie", addressBook), "Charlie");
        ActorRef<Customer.Message> marie = this.getContext().spawn(Customer.create("Marie", addressBook), "Marie");

        //Adding Customers to the AddressBook
        addressBook.tell(new AddressBook.AddCustomer(alice, "Alice"));
        addressBook.tell(new AddressBook.AddCustomer(bob, "Bob"));
        addressBook.tell(new AddressBook.AddCustomer(charlie, "Charlie"));
        addressBook.tell(new AddressBook.AddCustomer(marie, "Marie"));


        List<ActorRef<Customer.Message>> customers = new ArrayList<>();
        customers.add(alice);
        customers.add(bob);
        customers.add(charlie);
        customers.add(marie);

        //Creating the Distribution Center with a list of all Customers
        ActorRef<DistributionCenter.Message> distributionCenter = this.getContext().spawn(DistributionCenter.create(customers), "DistributionCenter");
        distributionCenter.tell(new DistributionCenter.Start());
        //#create-actors

        return this;
    }
}
