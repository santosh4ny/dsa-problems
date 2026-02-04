package com.dsa.day4;

public class BestTimeToBuyAndSellStocks {
    /*
Problem Description

Say you have an array, A, for which the ith element is the price of a given stock on day i.
If you were only permitted to complete at most one transaction
(ie, buy one and sell one share of the stock), design an algorithm to find the maximum profit.

Return the maximum possible profit.



Problem Constraints

0 <= A.size() <= 700000



1 <= A[i] <= 107





Input Format

The first and the only argument is an array of integers, A.


Output Format

Return an integer, representing the maximum possible profit.


Example Input

Input 1:
A = [1, 2]
Input 2:

A = [1, 4, 5, 2, 4]


Example Output

Output 1:
1
Output 2:

4


Example Explanation

Explanation 1:
Buy the stock on day 0, and sell it on day 1.
Explanation 2:

Buy the stock on day 0, and sell it on day 2.
     */
    public static void main(String[] args) {
        int[] stockPrices = {1, 4, 5, 2, 4};
        System.out.println("Max profit is : "+bestTimeToBuyAndSellStocks(stockPrices));
    }
    private static int bestTimeToBuyAndSellStocks(int[] stockPrices){
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for(int i=0; i<stockPrices.length; i++){
            min = Math.min(stockPrices[i], min);
            max = Math.max(stockPrices[i], max);
        }
        return max - min;
    }
}
