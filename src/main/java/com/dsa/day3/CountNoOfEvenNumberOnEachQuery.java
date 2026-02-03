package com.dsa.day3;

import java.util.Arrays;

public class CountNoOfEvenNumberOnEachQuery {
    /**
     * Given a number N and queries Q, count number of all even numbers
     * on each of the queries.
     */
    public static void main(String[] args) {
        int[] arr = {2, 4, 5, 6, 7, 8};
        int[][] queries = {
                {2, 5},
                {4, 5},
                {0, 4}
        };
        System.out.println("Count of Even number is :"+ Arrays.toString(countOfEvenNumber(arr, queries)));
    }
    private static int[] countOfEvenNumber(int[] arr, int[][] queries){
        int[] pf = new int[arr.length];
        int[] result = new int[queries.length];
        if(arr[0] % 2 == 0){
            pf[0] = 1;
        }else{
            pf[0] = 0;
        }
        for(int i=1; i<arr.length; i++){
            if(arr[i] % 2 == 0){
                pf[i] = pf[i-1] + 1;
            }else {
                pf[i] = pf[i - 1];
            }
        }
        for(int i=0; i<queries.length; i++){
            int left = queries[i][0];
            int right = queries[i][1];
            if(left != 0){
                result[i] = pf[right] - pf[left - 1];
            }else{
                result[i] = pf[right];
            }

        }
        return result;
    }
}
