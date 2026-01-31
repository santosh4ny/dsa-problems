package com.dsa.day1;

public class SquareRoot {
    public static void main(String[] args) {
        System.out.println("Square root of number: "+
                36 + " is "+ sqrt(36));
        System.out.println("Square root of number: "+
                15 + " is "+ sqrt(15));
    }
    private static int sqrt(int number){
        int ans = 0;
        int i=1;
        while(i * i <= number){
            if(i * i < number){
                ans = i;
            }else if(i * i == number){
                ans = i;
            }
            i++;
        }
        return ans;
    }
}
