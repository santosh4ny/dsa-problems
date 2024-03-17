package com.dsa.solution;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TwoSum {
    // Bruteforce solution of Two Sum
    public static int[] findTwoSum(int[] arr, int target){
        if(arr.length < 2)
            return null;
        for(int p1=0; p1<arr.length; p1++){
            int numberToFind = target - arr[p1];
            for(int p2=p1+1; p2<arr.length; p2++){
                if(numberToFind == arr[p2]){
                    return new int[]{p1, p2};
                }
            }
        }
        return null;
    }

    // Optimized Solution
    public static int[] findTwoSumWithOptimized(int[] arr, int target){
        Map<Integer, Integer> map =  new HashMap<>();
        if(arr.length< 2)
            return null;
        for(int p1=0; p1<arr.length; p1++){
            int numberToFind = target - arr[p1];
            if(map.containsKey(numberToFind)){
                return new int[]{map.get(numberToFind), p1};
            }
            map.put(arr[p1], p1);
        }

        return null;
    }

    public static void main(String[] args) {
        int[] arr = new int[]{2,7,11,15};
        int target = 9;
         Arrays.stream(findTwoSum(arr, target)).forEach(System.out::println);
        System.out.println("======= Optimized solution below ========");
         Arrays.stream(findTwoSumWithOptimized(arr, target)).forEach(System.out::println);
    }


}
