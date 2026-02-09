package com.dsa.day5;

import java.util.ArrayList;
import java.util.List;

public class AlternatingSubArray {
    /*
 Problem Description

You are given an integer array A of length N comprising of 0's & 1's, and an integer B.
You have to tell all the indices of array A that can act as a center of 2 * B + 1 length 0-1
alternating subarray.
A 0-1 alternating array is an array containing only 0's & 1's, and having no adjacent 0's or 1's.
For e.g. arrays [0, 1, 0, 1], [1, 0] and [1] are 0-1 alternating, while [1, 1] and [0, 1, 0, 0, 1]
are not.

Problem Constraints

1 <= N <= 103

A[i] equals to 0 or 1.

0 <= B <= (N - 1) / 2
Input Format

First argument is an integer array A.

Second argument is an integer B.
Output Format

Return an integer array containing indices(0-based) in sorted order.
 If no such index exists, return an empty integer array.
Example Input
Input 1:
 A = [1, 0, 1, 0, 1]
 B = 1
Input 2:
 A = [0, 0, 0, 1, 1, 0, 1]
 B = 0
Example Output

Output 1:

 [1, 2, 3]
Output 2:

 [0, 1, 2, 3, 4, 5, 6]


Example Explanation

Explanation 1:

 Index 1 acts as a centre of alternating sequence: [A0, A1, A2]
 Index 2 acts as a centre of alternating sequence: [A1, A2, A3]
 Index 3 acts as a centre of alternating sequence: [A2, A3, A4]
Explanation 2:

 Each index in the array acts as the center of alternating sequences of lengths 1.
     */
    public static void main(String[] args) {
        int [] nums = {1, 0, 1, 0, 1};
        int b = 1;
        System.out.println("Alternate array is : "+alternateSubArray(nums, b));
        int [] nums1 = {0, 0, 0, 1, 1, 0, 1};
        int b1 = 0;
        System.out.println("Alternate array second is : "+alternateSubArray(nums1, b1));
    }
    private static List<Integer> alternateSubArray(int[] nums, int b){
        List<Integer> result = new ArrayList<>();
        for(int i=b; i<nums.length-b; i++){
            boolean left = checkForAlternate(nums, i, b, "left");
            boolean right = checkForAlternate(nums, i, b, "right");
            if(left && right){
                result.add(i);
            }
        }
        return result;
    }
    private static boolean checkForAlternate(int[] nums, int index, int b, String direction){
        boolean flag = true;
        if(direction.equalsIgnoreCase("left")){
            int curr = nums[index];
            for(int j=index-1; j>= index -b; j--){
                if(curr != nums[j]){
                    curr = nums[j];
                    continue;
                }else{
                    flag= false;
                }
            }
        }else if(direction.equalsIgnoreCase("right")){
            int curr = nums[index];
            for(int j=index + 1; j < index + b + 1; j++){
                if(curr != nums[j]){
                    curr = nums[j];
                    continue;
                }else{
                    flag = false;
                }
            }
        }

        return flag;
    }
}
