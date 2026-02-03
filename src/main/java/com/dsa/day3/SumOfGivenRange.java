package com.dsa.day3;

import java.util.Arrays;

public class SumOfGivenRange {
    /**
     * Given n Array Elements and Q Queries on same array
     * For each query calculate, sum of all element in the given range
     */
    public static void main(String[] args) {
        int [] arr = {1, 2, 3, 4, 5};
        int[][] range = {
                {0, 3},
                {1, 2}
        };
        System.out.println("Range sum using Prefix sum is: "+ Arrays.toString(rangeSum(arr, range)));
    }
    // i am going to solve this using prefix sum
    private static int[] rangeSum(int[] arr, int[][]query){
        //1. find the prefix sum
        int[] result = new int[query.length];
        int[] prefixSum = new int[arr.length];
        prefixSum[0] = arr[0];
        for(int i=1; i<arr.length; i++){
            prefixSum[i] = prefixSum[i-1] + arr[i];
        }
        //2. iterate the query and add the sum into result array.
        for(int i=0; i<query.length; i++){
            int left = query[i][0];
            int right = query[i][1];
            int sum = 0;
            if(left != 0){
                sum = prefixSum[right] - prefixSum[left-1];
            }else{
                sum = prefixSum[right];
            }
            result[i]= sum;
        }
        return result;
    }
}
