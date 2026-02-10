package com.dsa.day6;

import java.util.Arrays;

public class MatrixTranspose {
    /*
    Problem Description

Given a 2D integer array A, return the transpose of A.
The transpose of a matrix is the matrix flipped over its main diagonal,
switching the matrix's row and column indices.


Problem Constraints

1 <= A.size() <= 1000






1 <= A[i].size() <= 1000

1 <= A[i][j] <= 1000






Input Format

First argument is a 2D matrix of integers.





Output Format

You have to return the Transpose of this 2D matrix.



Example Input

Input 1:




A = [[1, 2, 3],
     [4, 5, 6],
     [7, 8, 9]]
Input 2:

A = [[1, 2],[1, 2],[1, 2]]

Example Output

Output 1:
[[1, 4, 7],
[2, 5, 8],
[3, 6, 9]]
Output 2:

[[1, 1, 1], [2, 2, 2]]

Example Explanation

Explanation 1:

Clearly after converting rows to column and columns to rows of [[1, 2, 3],[4, 5, 6],[7, 8, 9]]
 we will get [[1, 4, 7], [2, 5, 8], [3, 6, 9]].
Explanation 2:

After transposing the matrix, A becomes [[1, 1, 1], [2, 2, 2]]
     */
    public static void main(String[] args) {
        int[][] arr = {
                {1, 2, 3},
                {4, 5, 6},
                {7, 8, 9}
        };
        for(int[] matrix : transposeMatrix(arr)){
            System.out.println(Arrays.toString(matrix));
        }
    }

    private static int[][] transposeMatrix(int[][] matrix){
        int rowLen = matrix.length;
        int colLen = matrix[0].length;
            int row = 0;
            for(int col=0; col<colLen; col++){
                    int temp = matrix[row][col];
                    matrix[row][col] = matrix[col][row];
                    matrix[col][row] = temp;
            }
            int col = colLen -1;
            for(int i = 1; i<rowLen; i++){
                int temp = matrix[i][col];
                matrix[i][col] = matrix[col][i];
                matrix[col][i] = temp;
            }
        return matrix;
    }
}
