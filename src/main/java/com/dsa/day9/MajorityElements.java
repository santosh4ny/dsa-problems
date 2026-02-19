package com.dsa.day9;

public class MajorityElements {
    /*
Problem Description

Given an array of size N, find the majority element.
The majority element is the element that appears more than floor(n/2) times.
You may assume that the array is non-empty and the majority element always exists in the array.



Problem Constraints

1 <= N <= 5*105
1 <= num[i] <= 109


Input Format
Only argument is an integer array.
Output Format
Return an integer.
Example Input
Input 1:
[2, 1, 2]
Input 2:
[1, 1, 1]
Example Output
Output 1:
2
Output 2:
1
Example Explanation

For Input 1:
2 occurs 2 times which is greater than 3/2.
For Input 2:
 1 is the only element in the array, so it is majority
     */
    public static void main(String[] args) {
        int [] nums = {2, 1, 2};
        System.out.println("Majority Element is: "+majorityElement(nums));
        int [] nums1 = {1, 1, 1};
        System.out.println("Majority Element is: "+majorityElement(nums1));
        int [] nums2 = {3, 1, 3, 1, 3, 1, 3, 1, 3};
        System.out.println("Majority Element is: "+majorityElement(nums2));

    }
    private static int majorityElement(int[] nums){
        int ele = nums[0];
        int freq = 1;
        for(int i=1; i<nums.length; i++){
            if(freq == 0){
                ele = nums[i];
            }
            if(ele == nums[i]){
                freq++;
            }else{
                freq--;
            }
        }
        int count = 0;
        for(int i=0; i<nums.length; i++){
            if(ele == nums[i]){
                count++;
            }
        }
        if(count > Math.floor(nums.length/2)){
            return ele;
        }
        return -1;
    }
}
