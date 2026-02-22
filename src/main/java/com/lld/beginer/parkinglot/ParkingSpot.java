package com.lld.beginer.parkinglot;

/*
Why i have usd encapsulation? To protect state from corruption.
 */
public class ParkingSpot {
    private int spotNumber;
    private VehicleType supportedType;
    private boolean isFree;
    private Vehicle vehicle;

    public boolean assignVehicle(Vehicle vehicle){
        if(isFree && vehicle.getType() == supportedType){
            isFree = false;
            this.vehicle = vehicle;
            return true;
        }
        return false;
    }
    public void removeVehicle(){
        isFree = true;
        vehicle = null;
    }

    public boolean isFree() {
        return isFree;
    }

    public VehicleType getSupportedType() {
        return supportedType;
    }
}
