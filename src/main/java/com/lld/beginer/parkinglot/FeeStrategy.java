package com.lld.beginer.parkinglot;


/*
why i have created a strategy pattern?
Because tomorrow pricing might change.
 */
public interface FeeStrategy {
    double calculateFee(long duration);
}
