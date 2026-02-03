package com.dsa.day3;

import java.util.Arrays;

public class ProductArrayPuzzle {
    /**
     * Product Array Puzzle
     * Given an array of integers A, find and return the product array of the same size where the ith element of the product array will be equal to the product of all the elements divided by the ith element of the array.
     *
     * Note: It is always possible to form the product array with integer (32 bit) values. Solve it without using the division operator.
     *
     *
     * Input Format
     *
     * The only argument given is the integer array A.
     * Output Format
     *
     * Return the product array.
     * Constraints
     *
     * 2 <= length of the array <= 1000
     * 1 <= A[i] <= 10
     * For Example
     *
     * Input 1:
     *     A = [1, 2, 3, 4, 5]
     * Output 1:
     *     [120, 60, 40, 30, 24]
     *
     * Input 2:
     *     A = [5, 1, 10, 1]
     * Output 2:
     *     [10, 50, 5, 50]
     */
    public static void main(String[] args) {
        int [] arr = {5, 1, 10, 1};
        System.out.println("Product of Array is : "+ Arrays.toString(productArray(arr)));
        int [] arr1 = {1, 2, 3, 4, 5};
        System.out.println("Product of Array is : "+ Arrays.toString(productArray(arr1)));
    }
    private static int[] productArray(int[] arr){
        int product = 1;
        for(int item : arr){
            product *= item;
        }
        for (int i=0; i<arr.length; i++){
            arr[i] = product / arr[i];
        }
        return arr;
    }
}
