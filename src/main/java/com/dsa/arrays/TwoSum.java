package com.dsa.arrays;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class TwoSum {
    public static void main(String[] args) {
        /*
        Given an unsorted integer array, find a pair with the given sum in it.
        Each input can have multiple solutions. The output should match with either one of them.
        input: nums[] = {8, 7, 2,5,3,1}, target = 10
        output: (8, 2) or (7, 3)
        The solution can return pair in any order. If no pair with the given sum exists, the solution should return null.
        input: nums[] = {5, 2, 6, 8, 1, 9}
        output: null;

         */
        int[] nums = {8, 7, 2,5,3,1};
        int[] nums1 = {5, 2, 6, 8, 1, 9};
        int target = 10;
        int target1 = 12;
        System.out.println("Pair Sum is : "+ findPair(nums, target));
        System.out.println("Pair Sum is : "+ findPair(nums1, target1));
        System.out.println("Pair Sum with optimized is : "+ findPairWithOptimized(nums, target));
        System.out.println("Pair Sum with optimized is : "+ findPairWithOptimized(nums1, target1));
    }
    private static List<List<Integer>> findPair(int[] nums, int target){
        // create a List<Integer> to store the current pair.
        List<List<Integer>> result = new ArrayList<>();
        for(int i=0; i<nums.length; i++){
            for(int j = i+1; j < nums.length; j++){
                if(nums[i] + nums[j] == target){
                    List<Integer> currentPair = new ArrayList<>();
                    currentPair.add(nums[i]);
                    currentPair.add(nums[j]);
                    result.add(currentPair);
                }
            }
        }
        if(result.size()> 0){
            return result;
        }
        return null;
    }

    private static List<List<Integer>> findPairWithOptimized(int[] nums, int target){
        List<List<Integer>> result = new ArrayList<>();
        HashMap<Integer, Integer> currentPair = new HashMap<>();
        for(int i=0; i<nums.length; i++){
            int rest = target - nums[i];
            if(currentPair.containsKey(rest)){
                List<Integer> current = new ArrayList<>();
                int index = currentPair.get(rest);
                current.add(nums[index]);
                current.add(nums[i]);
                result.add(current);
            }
            currentPair.put(nums[i], i);
        }
        if(result.size() > 0){
            return result;
        }
        return null;
    }
}
