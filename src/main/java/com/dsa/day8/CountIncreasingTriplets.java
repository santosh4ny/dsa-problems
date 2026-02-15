package com.dsa.day8;

public class CountIncreasingTriplets {
    /*
Problem Description
You are given an array A of N elements.
Find the number of triplets i,j and k such that i<j<k
and A[i]<A[j]<A[k]
Problem Constraints
1 <= N <= 103
1 <= A[i] <= 109
Input Format
First argument A is an array of integers.
Output Format
Return an integer.
Example Input
Input 1:
A = [1, 2, 4, 3]
Input 2:
A = [2, 1, 2, 3]
Example Output
Output 1:
2
Output 2:
1
Example Explanation

For Input 1:
The triplets that satisfy the conditions are [1, 2, 3] and [1, 2, 4].
For Input 2:

The triplet that satisfy the conditions is [1, 2, 3].
     */
    public static void main(String[] args) {
        int[] nums = {1, 2, 4, 3};
        int[] nums1 = {2, 1, 2, 3};
        System.out.println("Number of triplets: "+countIncreasingTriplets(nums));
        System.out.println("Number of triplets: "+countIncreasingTriplets(nums1));

        System.out.println("Number of triplets Optimized: "+countIncreasingTripletsOptimized(nums));
        System.out.println("Number of triplets Optimized: "+countIncreasingTripletsOptimized(nums1));
    }

    private static int countIncreasingTripletsOptimized(int[] nums){
        int totalCount = 0;
        if(nums.length < 3){
            return totalCount;
        }
        for(int j=1; j<nums.length; j++){
            int leftCount=0, rightCount=0;
            for(int i=0; i<j; i++){
                if(nums[i] < nums[j]){
                    leftCount++;
                }
            }

            for(int k=j+1; k<nums.length; k++){
                if(nums[j] < nums[k]){
                    rightCount++;
                }
            }
            totalCount += leftCount * rightCount;
        }

        return totalCount;
    }
    private static int countIncreasingTriplets(int[] nums){
        int count=0;
        for(int i=0; i<nums.length; i++){
            for(int j=i+1; j<nums.length; j++){
                for(int k = j+1; k<nums.length; k++){
                    if(nums[i] < nums[j] && nums[j] < nums[k]){
                        count++;
                    }
                }
            }
        }

        return count;
    }
}
