package com.lld.beginer.parkinglot;

public class Ticket {
    private String ticketNumber;
    private Vehicle vehicle;
    private ParkingSpot spot;

    public Ticket(Vehicle vehicle, ParkingSpot spot) {
        this.vehicle = vehicle;
        this.spot = spot;
    }
}
