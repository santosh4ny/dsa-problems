package com.dsa.day9;

import org.springframework.stereotype.Service;

public class SpecialIndices {
    /*
Problem Description
Given an array, arr[] of size N, the task is to find the count of array
indices such that removing an element from these indices makes the sum
of even-indexed and odd-indexed array elements equal.

Problem Constraints

1 <= N <= 105
-105 <= A[i] <= 105
Sum of all elements of A <= 109
Input Format
First argument contains an array A of integers of size N
Output Format
Return the count of array indices such that removing an element from these indices makes
the sum of even-indexed and odd-indexed array elements equal.
Example Input

Input 1:
A = [2, 1, 6, 4]
Input 2:
A = [1, 1, 1]
Example Output

Output 1:
1
Output 2:
3
Example Explanation

Explanation 1:
Removing arr[1] from the array modifies arr[] to { 2, 6, 4 } such that, arr[0] + arr[2] = arr[1].
Therefore, the required output is 1.
Explanation 2:

Removing arr[0] from the given array modifies arr[] to { 1, 1 } such that arr[0] = arr[1]
Removing arr[1] from the given array modifies arr[] to { 1, 1 } such that arr[0] = arr[1]
Removing arr[2] from the given array modifies arr[] to { 1, 1 } such that arr[0] = arr[1]
Therefore, the required output is 3.
     */
    public static void main(String[] args) {
        int[] arr = {2, 1, 6, 4};
        int[] arr1 = {1, 1, 1, 1, 1};
        System.out.println(specialIndices(arr));
        System.out.println(specialIndices(arr1));
    }
    private static int specialIndices(int[] arr){
        int count = 0;
        int[] evenPf = new int[arr.length];
        int[] oddPf = new int[arr.length];
        evenPf[0] = arr[0];
        oddPf[0] = 0;
        for(int i=1; i<arr.length; i++){
            if(i % 2 == 0){
                evenPf[i] = evenPf[i-1] + arr[i];
                oddPf[i] = oddPf[i-1];
            }else{
                evenPf[i] = evenPf[i-1];
                oddPf[i] = oddPf[i-1]  + arr[i];
            }
        }
        int n = arr.length -1;
        for(int i=0; i<arr.length; i++){
            int evenSum = 0, oddSum = 0;
            if(i != 0){
                evenSum = evenPf[i-1] + oddPf[n] - oddPf[i];
                oddSum = oddPf[i-1] + evenPf[n] - evenPf[i];
            }else{
                evenSum = oddPf[n] - oddPf[i];
                oddSum = evenPf[n] - evenPf[i];
            }
            if(evenSum == oddSum) {
                count++;
            }
        }
        return count;

    }
}

