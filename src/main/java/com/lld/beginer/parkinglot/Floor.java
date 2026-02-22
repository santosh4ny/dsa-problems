package com.lld.beginer.parkinglot;

import java.util.List;

public class Floor {
    private int floorNumber;
    private List<ParkingSpot> spots;

    public ParkingSpot getAvailableSpot(VehicleType type){
        for(ParkingSpot spot : spots){
            if(spot.isFree() && spot.getSupportedType() == type){
                return spot;
            }
        }
        return null;
    }
}
