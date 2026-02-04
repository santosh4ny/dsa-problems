package com.dsa.arrays;

import java.util.ArrayList;
import java.util.List;

public class ZeroSum {
    /*
    Given an integer aray, check if it contains a contiguous subarray having zero sum.
    input: {3, 4, -7, 3, 1, 3, 1, -4, -2, -2}
    output: true
    Explanation:
    The subarrays with zero-sum are
    {3, 4, -7}
    {4, -7, 3}
    {-7, 3, 1, 3}
    ...
     */
    public static void main(String[] args) {
        int[] nums = {3, 4, -7, 3, 1, 3, 1, -4, -2};
        System.out.println(hasZeroSumSubarray(nums));
    }
    private static boolean hasZeroSumSubarray(int[] nums){
        // brute force i am going to use first
        List<List<Integer>> result = new ArrayList<>();
        for(int i=0; i<nums.length; i++){
            int sum = 0;
            for(int j = i; j<nums.length; j++){
                sum += nums[j];
                if(sum == 0){
                    List<Integer> current = new ArrayList<>();
                    current.add(i);
                    current.add(j);
                    result.add(current);
                    System.out.println("The sum zero index start with : "+i);
                    System.out.println("The sum zero index End with : "+j);
                    // Why i have used above approach is to when someone will ask to return all the pairs
                    // then just we will return it.
                    return true;
                }
            }

        }
        return false;
    }
}
