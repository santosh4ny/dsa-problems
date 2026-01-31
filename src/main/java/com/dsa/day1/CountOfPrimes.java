package com.dsa.day1;

public class CountOfPrimes {
    /**
     * You will be given an integer n.
     * You need to return the count of
     * prime numbers less than or equal to n.
     */
    public static void main(String[] args) {
        System.out.println("Count of Prime for Number 19 is : "+ countOfPrimes(19));
    }
    private static int countOfPrimes(int number){
        int numberOfPrime = 0;
        for(int i =2; i<= number; i++){
            if(countOfFactor(i) == 2){
                numberOfPrime += 1;
            }
        }
        return numberOfPrime;
    }
    private static int countOfFactor(int number){
        int count = 0;
        for(int i =1; i*i <= number; i++){
            if(number % i == 0){
                if(i == number/i){
                    count += 1;
                }else{
                    count += 2;
                }
            }
        }
        return count;
    }
}
