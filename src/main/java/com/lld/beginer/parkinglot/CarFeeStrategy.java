package com.lld.beginer.parkinglot;

public class CarFeeStrategy implements FeeStrategy{
    @Override
    public double calculateFee(long duration) {
        if(duration > 1){
            return 75;
        }
        return 50;
    }
}
