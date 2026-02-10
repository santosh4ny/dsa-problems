package com.dsa.day6;

import java.util.Arrays;

public class AntiDiagonals {
    /*
Problem Description
Give a N * N square matrix A, return an array of its anti-diagonals.
Look at the example for more details.
Problem Constraints
1<= N <= 1000
1<= A[i][j] <= 1e9
Input Format
Only argument is a 2D array A of size N * N.
Output Format
Return a 2D integer array of size (2 * N-1) * N, representing the anti-diagonals of input array A.
The vacant spaces in the grid should be assigned to 0.
Example Input
Input 1:
1 2 3
4 5 6
7 8 9
Input 2:
1 2
3 4
Example Output
Output 1:
1 0 0
2 4 0
3 5 7
6 8 0
9 0 0
Output 2:
1 0
2 3
4 0
Example Explanation
For input 1:
The first anti diagonal of the matrix is [1 ], the rest spaces shoud be filled with 0 making the row as [1, 0, 0].
The second anti diagonal of the matrix is [2, 4 ], the rest spaces shoud be filled with 0 making the row as [2, 4, 0].
The third anti diagonal of the matrix is [3, 5, 7 ], the rest spaces shoud be filled with 0 making the row as [3, 5, 7].
The fourth anti diagonal of the matrix is [6, 8 ], the rest spaces shoud be filled with 0 making the row as [6, 8, 0].
The fifth anti diagonal of the matrix is [9 ], the rest spaces shoud be filled with 0 making the row as [9, 0, 0].
For input 2:

The first anti diagonal of the matrix is [1 ], the rest spaces shoud be filled with 0 making the row as [1, 0, 0].
The second anti diagonal of the matrix is [2, 4 ], the rest spaces shoud be filled with 0 making the row as [2, 4, 0].
The third anti diagonal of the matrix is [3, 0, 0 ], the rest spaces shoud be filled with 0 making the row as [3, 0, 0].
     */

    public static void main(String[] args) {
        int[][] arr = {
                {1, 2, 3},
                {4, 5, 6},
                {7, 8, 9}
        };
        System.out.println("Anti Diagonal : ");
        for (int[] ints : antiDiagonal(arr)) {
            System.out.println(Arrays.toString(ints));;
        }
    }

    private static int[][] antiDiagonal(int[][]arr){
        int rowLen = arr.length, colLen = arr[0].length;
        int[][] result = new int[rowLen + colLen -1][colLen];
        for(int row = 0; row<rowLen+colLen -1; row++){
            for(int col =0; col<colLen; col++){
                result[row][col] = 0;
            }
        }
        int row = 0;
        int rowIndex = 0;
        for(int col=0; col<colLen; col++){
            int i = row;
            int j = col;
            int colIndex = 0;
            while(i < arr.length && j>=0){
                result[rowIndex][colIndex] = arr[i][j];
                i++;
                j--;
                colIndex++;
            }
            rowIndex++;
        }
        // Here i am starting increasing row and fixing col size;
        int col = colLen -1;
        for(row = 1; row<rowLen; row++){
            int i=row, j = col, indexCol=0;

            while(i<rowLen && j>=0){
                result[rowIndex][indexCol] = arr[i][j];
                i++;
                j--;
                indexCol++;
            }
            rowIndex++;
        }
        return result;
    }
}
