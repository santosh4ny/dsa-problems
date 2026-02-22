package com.lld.beginer.parkinglot;

import java.util.List;

/*
Here i have use facade, because it hides complexity from client
 */
public class ParkingLot {
    private List<Floor> floors;

    public Ticket parkVehicle(Vehicle vehicle){
        for(Floor floor : floors){
            ParkingSpot spot = floor.getAvailableSpot(vehicle.getType());
            if(spot != null){
                spot.assignVehicle(vehicle);
                return new Ticket(vehicle, spot);
            }
        }
        throw new RuntimeException("Not Spot Available");
    }
}
