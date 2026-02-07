package com.dsa.day5;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SubArrayInGivenRange {
    /*
 Problem Description
Given an array A of length N, return the subarray from B to C.
Problem Constraints
1 <= N <= 105
1 <= A[i] <= 109
0 <= B <= C < N
Input Format
The first argument A is an array of integers
The remaining argument B and C are integers.
Output Format
Return a subarray
Example Input
Input 1:
A = [4, 3, 2, 6]
B = 1
C = 3
Input 2:
A = [4, 2, 2]
B = 0
C = 1
Example Output
Output 1:
[3, 2, 6]
Output 2:
[4, 2]
Example Explanation

Explanation 1:
The subarray of A from 1 to 3 is [3, 2, 6].
Explanation 2:
The subarray of A from 0 to 1 is [4, 2].
     */
    public static void main(String[] args) {
        int[] nums = {4, 3, 2, 6};
        int startIndex = 1, endIndex = 3;
        System.out.println(subArraysInGivenRange(nums, startIndex, endIndex));
        System.out.println("=================");
        System.out.println(Arrays.toString(largeSubarrayInGivenRange(nums, startIndex, endIndex)));
    }
    private static int[] largeSubarrayInGivenRange(int[] nums, int startIndex, int endIndex){
        int n = nums.length;
        int[] result = new int[endIndex - startIndex + 1];
        int index=0;
        for(int i= startIndex; i<=endIndex; i++){
            result[index] = nums[i];
            index++;
        }
        return result;
    }
    private static List<List<Integer>> subArraysInGivenRange(int[] nums, int startIndex, int endIndex){
        int n = nums.length;
        List<List<Integer>> result = new ArrayList<>();
        for(int i = startIndex; i < n; i++){
            for(int j = i; j<=endIndex; j++){
                List<Integer> currentSubArray = new ArrayList<>();
                for(int k=i; k<=j; k++ ){
                    currentSubArray.add(nums[k]);
                }
                result.add(currentSubArray);
            }
        }
        return result;
    }
}
