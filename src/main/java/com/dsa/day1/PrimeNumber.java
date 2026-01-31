package com.dsa.day1;

public class PrimeNumber {
    public static void main(String[] args) {
        int number = 7;
        if(isPrimeNuber(number)){
            System.out.println(number + " is a prime number");
        }else {
            System.out.println(number + " is not a prime number");
        }
    }

    private static boolean isPrimeNuber(int number){
        int count = 0;
        for(int i=1; i*i <= number; i++){
            if(number % i == 0){
                if(number/i == i){
                    count += 1;
                }else {
                    count += 2;
                }
            }
        }

        if(count == 2){
            return true;
        }
        return false;
    }
}
