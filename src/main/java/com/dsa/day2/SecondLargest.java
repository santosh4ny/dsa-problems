package com.dsa.day2;

public class SecondLargest {
    /**
     * You are given an integer array A. You have to find the second largest element/value in the array or report that no such element exists.
     *
     *
     * Problem Constraints
     *
     * 1 <= |A| <= 105
     *
     *
     * 0 <= A[i] <= 109
     *
     *
     *
     *
     *
     * Input Format
     *
     * The first argument is an integer array A.
     *
     *
     *
     *
     *
     * Output Format
     *
     * Return the second largest element. If no such element exist then return -1.
     *
     *
     *
     * Example Input
     *
     * Input 1:
     *
     *  A = [2, 1, 2]
     * Input 2:
     *
     *  A = [2]
     *
     *
     * Example Output
     *
     * Output 1:
     *
     *  1
     * Output 2:
     *
     *  -1
     */
    public static void main(String[] args) {
        int [] arr = {2, 1, 2};
        System.out.println("Second Largest number is : "+secondLargestElement(arr));
        int [] arr1 = {2};
        System.out.println("Second Largest number is : "+secondLargestElement(arr1));
    }
    private static int secondLargestElement(int[] arr){
        int largest = arr[0], secondLargest = -1;
        if(arr.length < 2){
            return secondLargest;
        }
        for(int i=1; i<arr.length; i++){
            if(arr[i] > largest){
                secondLargest = largest;
                largest = arr[i];
            }
            if(arr[i] > secondLargest && arr[i] < largest){
                secondLargest = arr[i];
            }
        }
        return secondLargest;
    }
}
