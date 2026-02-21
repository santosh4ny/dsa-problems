package com.dsa.day10;

public class SingleNumber {
    /*
Problem Description

Given an array of integers A, every element appears twice except for one.
Find that integer that occurs once.
NOTE: Your algorithm should have a linear runtime complexity.
Could you implement it without using extra memory?



Problem Constraints

1 <= |A| <= 2000000

0 <= A[i] <= INTMAX



Input Format

The first and only argument of input contains an integer array A.



Output Format

Return a single integer denoting the single element.



Example Input

Input 1:

 A = [1, 2, 2, 3, 1]
Input 2:

 A = [1, 2, 2]


Example Output

Output 1:

 3
Output 2:

 1


Example Explanation

Explanation 1:

3 occurs once.
Explanation 2:

1 occurs once.
     */
    public static void main(String[] args) {
        int[] nums = {1, 2, 2, 3, 1};
        System.out.println("Single Number is : "+ findSingleNumber(nums));
        int [] nums1 = {1, 2, 2};
        System.out.println("Single Number is : "+ findSingleNumber(nums1));

    }
    private static int findSingleNumber(int[] nums){
        if(nums.length == 0){
            return 0;
        }
        int result = 0;
        for(int i=0; i<nums.length; i++){
            result = result ^ nums[i];
        }
        return result;
    }
}
