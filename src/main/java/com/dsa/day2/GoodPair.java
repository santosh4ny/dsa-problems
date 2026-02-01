package com.dsa.day2;

public class GoodPair {
    /**
     * Given an array A and an integer B. A pair(i, j)
     * in the array is a good pair if i != j and (A[i] + A[j] == B).
     * Check if any good pair exist or not.
     * nput 1:
     *
     * A = [1,2,3,4]
     * B = 7
     * Input 2:
     *
     * A = [1,2,4]
     * B = 4
     * Input 3:
     *
     * A = [1,2,2]
     * B = 4
     *
     *
     * Example Output
     *
     * Output 1:
     *
     * 1
     * Output 2:
     *
     * 0
     * Output 3:
     *
     * 1
     */
    public static void main(String[] args) {
        int [] arr = {1,2,3,4};
        int [] arr1 = {1,2,4};
        System.out.println(" Pair is available : "+isGoodPair(arr, 7));
        System.out.println(" Pair is available : "+isGoodPair(arr1, 4));
    }
    private static int isGoodPair(int[] arr, int target){
        for(int i=0; i<arr.length; i++){
            for(int j = i + 1; j< arr.length; j++){
                if(arr[i] + arr[j] == target){
                    return 1;
                }
            }
        }
        return 0;
    }
}
