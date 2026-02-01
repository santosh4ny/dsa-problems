package com.dsa.day2;

import java.util.Arrays;

public class ReverseInARange {
    /**
     * Given an array A of N integers and also given two
     * integers B and C. Reverse the elements of the array A
     * within the given inclusive range [B, C].
     * Input Format
     *
     * The first argument A is an array of integer.
     * The second and third arguments are integers B and C
     *
     *
     * Output Format
     *
     * Return the array A after reversing in the given range.
     *
     *
     * Example Input
     *
     * Input 1:
     *
     * A = [1, 2, 3, 4]
     * B = 2
     * C = 3
     * Input 2:
     *
     * A = [2, 5, 6]
     * B = 0
     * C = 2
     *
     *
     * Example Output
     *
     * Output 1:
     *
     * [1, 2, 4, 3]
     * Output 2:
     *
     * [6, 5, 2]
     *
     *
     * Example Explanation
     *
     * Explanation 1:
     *
     * We reverse the subarray [3, 4].
     * Explanation 2:
     *
     * We reverse the entire array [2, 5, 6].
     */
    public static void main(String[] args) {
        int[] arr = {1, 2, 3, 4};
        System.out.println(Arrays.toString(reverse(arr, 2, 3)));
    }
    private static int[] reverse(int[] arr, int startIndex, int endIndex){
        while(startIndex < endIndex){
            int temp = arr[startIndex];
            arr[startIndex] = arr[endIndex];
            arr[endIndex] = temp;
            startIndex++;
            endIndex--;
        }
        return arr;
    }
}
