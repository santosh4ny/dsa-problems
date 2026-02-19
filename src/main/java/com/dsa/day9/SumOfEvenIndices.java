package com.dsa.day9;

import java.util.Arrays;

public class SumOfEvenIndices {
    /*
Problem Description

You are given an array A of length N and Q queries given by the 2D array B of size Q*2.
 Each query consists of two integers B[i][0] and B[i][1].
For every query, the task is to calculate the sum of all even indices
in the range A[B[i][0]…B[i][1]].

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
[4, 8]
Output 2:
[10, 17]
     */
    public static void main(String[] args) {
        int[] A = {1, 2, 3, 4, 5};
        int[][] B = {
                {0,2},
                {1, 4}
        };
        System.out.println(Arrays.toString(sumOfEvenIndices(A, B)));
    }
    private static int[] sumOfEvenIndices(int[] A, int[][] B){
        int[] result = new int[B.length];
        int[] evenPf = new int[A.length];
        evenPf[0] = A[0];
        for(int i=1; i<A.length; i++){
            if(i % 2 == 0){
                evenPf[i] = evenPf[i-1] + A[i];
            }else{
                evenPf[i] = evenPf[i-1];
            }
        }
        for(int i=0; i<B.length; i++){
            int start = B[i][0];
            int end = B[i][1];
            if(start == 0){
                result[i] = evenPf[end];
            }else {
                result[i] = evenPf[end] - evenPf[start-1];
            }

        }
        return result;
    }
}
