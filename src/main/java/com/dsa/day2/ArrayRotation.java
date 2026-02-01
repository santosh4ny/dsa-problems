package com.dsa.day2;

import java.util.Arrays;

public class ArrayRotation {
    /**
     * Given an integer array A of size N and an integer B, you have to return the same array after rotating it B times towards the right.
     *
     *
     * Problem Constraints
     *
     * 1 <= N <= 105
     * 1 <= A[i] <=109
     * 1 <= B <= 109
     *
     *
     * Input Format
     *
     * The first argument given is the integer array A.
     * The second argument given is the integer B.
     *
     *
     * Output Format
     *
     * Return the array A after rotating it B times to the right
     *
     *
     * Example Input
     *
     * Input 1:
     *
     * A = [1, 2, 3, 4]
     * B = 2
     * Input 2:
     *
     * A = [2, 5, 6]
     * B = 1
     *
     *
     * Example Output
     *
     * Output 1:
     *
     * [3, 4, 1, 2]
     * Output 2:
     *
     * [6, 2, 5]
     *
     *
     * Example Explanation
     *
     * Explanation 1:
     *
     * Rotate towards the right 2 times - [1, 2, 3, 4] => [4, 1, 2, 3] => [3, 4, 1, 2]
     * Explanation 2:
     *
     * Rotate towards the right 1 time - [2, 5, 6] => [6, 2, 5]
     */
    public static void main(String[] args) {
        int[] arr = {1, 2, 3, 4};
        int[] arr1 = {2, 5, 6};
        int b = 2;
        int b1 = 1;
        rotate(arr, 0, arr.length -1);
        rotate(arr, 0, b -1);
        rotate(arr, b, arr.length -1);
        System.out.println(Arrays.toString(arr));
        rotate(arr1, 0, arr1.length -1);
        rotate(arr1, 0, b1 -1);
        rotate(arr1, b1, arr1.length -1);
        System.out.println(Arrays.toString(arr1));
    }
    private static void rotate(int[] arr, int startIndex, int endIndex){
        while(startIndex < endIndex){
            int temp = arr[startIndex];
            arr[startIndex] = arr[endIndex];
            arr[endIndex] = temp;
            startIndex++;
            endIndex--;
        }
    }
}
