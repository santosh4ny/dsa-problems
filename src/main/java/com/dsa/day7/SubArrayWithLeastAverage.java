package com.dsa.day7;

public class SubArrayWithLeastAverage {
    /*
Problem Description
Given an array A of size N, find the subarray of size B with the least average.
Problem Constraints
1 <= B <= N <= 105
-105 <= A[i] <= 105
Input Format
First argument contains an array A of integers of size N.
Second argument contains integer B.
Output Format
Return the index of the first element of the subarray of size B that has least average.
Array indexing starts from 0.
Example Input
Input 1:
A = [3, 7, 90, 20, 10, 50, 40]
B = 3
Input 2:
A = [3, 7, 5, 20, -10, 0, 12]
B = 2
Example Output

Output 1:
3
Output 2:
4
Example Explanation
Explanation 1:
Subarray between indexes 3 and 5
The subarray {20, 10, 50} has the least average
among all subarrays of size 3.
Explanation 2:

 Subarray between [4, 5] has minimum average
     */
    public static void main(String[] args) {
//        Input 1:
        int[] A = {3, 7, 90, 20, 10, 50, 40};
        int B = 3;
        System.out.println("Least Average : "+ subArrayWithLeastAverage(A, B));
//        Input 2:
        int[] A1 = {3, 7, 5, 20, -10, 0, 12};
        int B1 = 2;
        System.out.println("Least Average : "+ subArrayWithLeastAverage(A1, B1));
    }
    private static int subArrayWithLeastAverage(int[] arr, int b){
        int average = 0;
        int leastIndex = -1;
        int sum = 0;
        for(int i=0; i<b; i++){
            sum += arr[i];
        }
        int i=1;
        average = sum / b;
        int currentAvg = average;
        while(b < arr.length){
            currentAvg = (sum + arr[b] - arr[i-1])/b;
            if(currentAvg < average){
                average = currentAvg;
                leastIndex = i;
            }
            b++;
            i++;
        }
        return leastIndex;
    }
}
