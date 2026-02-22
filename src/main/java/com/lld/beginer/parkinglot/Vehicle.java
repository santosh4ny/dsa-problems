package com.lld.beginer.parkinglot;

/*
Why i have used abstract, because Car, Bike, Truck will extend
 */
public abstract class Vehicle {
    private String vehicleNumber;
    private VehicleType type;

    public Vehicle(String vehicleNumber, VehicleType type) {
        this.vehicleNumber = vehicleNumber;
        this.type = type;
    }

    public VehicleType getType(){
        return type;
    }
}
