package com.dsa.day2;

public class TimeToEquality {
    /**
     * Given an integer array A of size N. In one second, you can increase the value of one element by 1.
     *
     * Find the minimum time in seconds to make all elements of the array equal.
     *
     *
     * Problem Constraints
     *
     * 1 <= N <= 1000000
     * 1 <= A[i] <= 1000
     *
     *
     * Input Format
     *
     * First argument is an integer array A.
     *
     *
     * Output Format
     *
     * Return an integer denoting the minimum time to make all elements equal.
     *
     *
     * Example Input
     *
     * A = [2, 4, 1, 3, 2]
     *
     *
     * Example Output
     *
     * 8
     *
     *
     * Example Explanation
     *
     * We can change the array A = [4, 4, 4, 4, 4]. The time required will be 8 seconds.
     */
    public static void main(String[] args) {
        int[] arr = {2, 4, 1, 3, 2};
        System.out.println("Max time is required to equality is : "+ timeToEquality(arr));
        int[] arr1 = {4, 4, 4, 4, 4};
        System.out.println("Max time is required to equality is : "+ timeToEquality(arr1));

    }
    private static int timeToEquality(int[] arr){
        // find the max element

        // iterate and if element is less than max element then substract and add to the result
        int maxElement = arr[0];
        for(int i=1; i< arr.length; i++){
            if(maxElement < arr[i]){
                maxElement = arr[i];
            }
        }
        int equality = 0;
        for(int element : arr){
            if(element <= maxElement){
                equality += (maxElement - element);
            }
        }
        return equality;
    }
}
