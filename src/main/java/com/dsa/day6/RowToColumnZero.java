package com.dsa.day6;

import java.util.Arrays;

public class RowToColumnZero {
    /*
    Problem Description

You are given a 2D integer matrix A, make all the elements in a row or column zero if the A[i][j] = 0. Specifically, make entire ith row and jth column zero.



Problem Constraints

1 <= A.size() <= 103

1 <= A[i].size() <= 103

0 <= A[i][j] <= 103



Input Format

First argument is a 2D integer matrix A.



Output Format

Return a 2D matrix after doing required operations.



Example Input

Input 1:

[1,2,3,4]
[5,6,7,0]
[9,2,0,4]


Example Output

Output 1:

[1,2,0,0]
[0,0,0,0]
[0,0,0,0]


Example Explanation

Explanation 1:

A[2][4] = A[3][3] = 0, so make 2nd row, 3rd row, 3rd column and 4th column zero.
     */

    public static void main(String[] args) {
        int[][] arr = {
                {1,2,3,4},
                {5,6,7,0},
                {9,2,0,4}
        };
        for(int[] matrix : rowColZero(arr)){
            System.out.println(Arrays.toString(matrix));
        }
    }
    private static int[][] rowColZero(int[][] matrix){
        int[][] result = new int[matrix.length][matrix[0].length];
        for(int i=0; i<matrix.length; i++){
            for(int j=0; j<matrix[0].length; j++){
                result[i][j] = matrix[i][j];
            }
        }

        for(int i=0; i<matrix.length; i++){
            for(int j=0; j<matrix[0].length; j++){
                if(matrix[i][j] == 0){
                    // fix the row and make all column 0

                    for(int col = 0; col<matrix[0].length; col++){
                        result[i][col] = 0;
                    }
                    // fix the col and make all the row 0
                    for(int row = 0; row<matrix.length; row++){
                        result[row][j] = 0;
                    }
                }
            }
        }
        return result;
    }
}
