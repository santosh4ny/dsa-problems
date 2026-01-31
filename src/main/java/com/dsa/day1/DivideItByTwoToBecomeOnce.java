package com.dsa.day1;

public class DivideItByTwoToBecomeOnce {
    /**
     * Given a positive number, how many times we need to divide it by 2
     * to reduce it to 1;
     */
    public static void main(String[] args) {
        System.out.println("Count is : "+divideAndReduceToOne(8));
        System.out.println("Count is : "+divideAndReduceToOne(9));
        System.out.println("Count is : "+divideAndReduceToOne(25));
    }
    private static int divideAndReduceToOne(int number){
        int count = 0;
        while(number > 1){
            number = Math.floorDiv(number, 2);
            count += 1;
        }

        return count;
    }
}
