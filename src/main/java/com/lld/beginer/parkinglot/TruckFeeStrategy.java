package com.lld.beginer.parkinglot;

public class TruckFeeStrategy implements FeeStrategy{
    @Override
    public double calculateFee(long duration) {
        if(duration > 1){
            return 150;
        }

        return 100;
    }
}
