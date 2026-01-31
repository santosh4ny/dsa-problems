package com.dsa.day1;

public class CountOfFactors {
    public static void main(String[] args) {
        int number = 24;
        System.out.println("Count of Factor is :"+countOfFactors(number));
    }
    private static int countOfFactors(int number){
        int count = 0;
        for(int i = 1; i * i <= number; i++){
            if(number % i == 0){
                if(i == number/i){
                    count += 1;
                }else {
                    count += 2;
                }
            }
        }
        return count;
    }
}
