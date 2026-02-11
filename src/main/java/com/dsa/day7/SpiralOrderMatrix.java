package com.dsa.day7;

import java.util.Arrays;

public class SpiralOrderMatrix {
    /*
Problem Description
Given an integer A, generate a square matrix filled with elements
from 1 to A2 in spiral order and return the generated square matrix.
Problem Constraints
1 <= A <= 1000
Input Format
First and only argument is integer A
Output Format
Return a 2-D matrix which consists of the elements added in spiral order.
Example Input
Input 1:
1
Input 2:
2
Input 3:
5
Example Output
Output 1:
[ [1] ]
Output 2:

[ [1, 2],
  [4, 3] ]
Output 3:
[ [1,   2,  3,  4, 5],
  [16, 17, 18, 19, 6],
  [15, 24, 25, 20, 7],
  [14, 23, 22, 21, 8],
  [13, 12, 11, 10, 9] ]
Example Explanation
Explanation 1:
Only 1 is to be arranged.
Explanation 2:
1 --> 2
      |
      |
4<--- 3
Explanation 3:

     */
    public static void main(String[] args) {
        int len = 5;
        for(int[] arr : printMatrix(len)){
            System.out.println(Arrays.toString(arr));
        }
    }
    private static int[][] printMatrix(int len){
        int cnt = 1;
        int i=0, j=0;
        int[][] result =new int[len][len];
        // approach -- 0
        while(len>1){
            // add elements into first row
            for(int k=1; k<=len-1; k++){
                result[i][j]=cnt++;
                j++;
            }
            // add elements into first row
            for(int k=1; k<=len-1; k++){
                result[i][j]=cnt++;
                i++;
            }
            // add elements into first row
            for(int k=1; k<=len-1; k++){
                result[i][j]=cnt++;
                j--;
            }
            // add elements into first row
            for(int k=1; k<=len-1; k++){
                result[i][j]=cnt++;
                i--;
            }
            i++;
            j++;
            len = len -2;
        }
        if(len == 1){
            result[i][j]= cnt++;
        }
        return result;
    }


}
