package com.dsa.day9;

import java.util.Arrays;

public class SumOfOddIndices {
    /*
Problem Description

You are given an array A of length N and Q queries given by the 2D array B of size Q*2. Each query consists of two integers B[i][0] and B[i][1].
For every query, the task is to calculate the sum of all odd indices in the range A[B[i][0]…B[i][1]].

Note : Use 0-based indexing


Problem Constraints

1 <= N <= 105
1 <= Q <= 105
1 <= A[i] <= 100
0 <= B[i][0] <= B[i][1] < N


Input Format

First argument A is an array of integers.
Second argument B is a 2D array of integers.


Output Format

Return an array of integers.


Example Input

Input 1:
A = [1, 2, 3, 4, 5]
B = [   [0,2]
        [1,4]   ]
Input 2:
A = [2, 1, 8, 3, 9]
B = [   [0,3]
        [2,4]   ]


Example Output

Output 1:
[2, 6]
Output 2:
[4, 3]


Example Explanation

For Input 1:
The subarray for the first query is [1, 2, 3] whose sum of odd indices is 2.
The subarray for the second query is [2, 3, 4, 5] whose sum of odd indices is 6.
For Input 2:
The subarray for the first query is [2, 1, 8, 3] whose sum of odd indices is 4.
The subarray for the second query is [8, 3, 9] whose sum of odd indices is 3.
     */
    public static void main(String[] args) {
        int[] A = {1, 2, 3, 4, 5};
        int[][] B = {
                {0,2},
                {1,4}
        };
        System.out.println(Arrays.toString(sumOfOddIndices(A, B)));

    }
    private static int[] sumOfOddIndices(int[] arr, int[][] query){
        int[] result  = new int[query.length];
        int[] oddPf = new int[arr.length];
        oddPf[0] = 0;
        for(int i=1; i<arr.length; i++){
            if(i % 2 != 0){
                oddPf[i] = oddPf[i-1] + arr[i];
            }else{
                oddPf[i] = oddPf[i-1];
            }
        }

        for(int i=0; i<query.length; i++){
            int start = query[i][0];
            int end = query[i][1];
            if(start == 0){
                result[i] = oddPf[end];
            }else{
                result[i] = oddPf[end] - oddPf[start -1];
            }
        }
        return result;
    }
}
