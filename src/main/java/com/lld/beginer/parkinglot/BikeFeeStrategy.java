package com.lld.beginer.parkinglot;

public class BikeFeeStrategy implements FeeStrategy{
    @Override
    public double calculateFee(long duration) {
        if(duration > 1){
            return 45;
        }
        return 30;
    }
}
