package com.dsa.day10;

public class XORSum {
    /*
Problem Description

Given two integers A and B. Find the minimum value (A ⊕ X) + (B ⊕ X) that can be achieved for any X.

where P ⊕ Q is the bitwise XOR operation of the two numbers P and Q.

Note: Bitwise XOR operator will return 1, if both bits are different. If bits are same, it will return 0.


Problem Constraints

1 <= A, B <= 109


Input Format

The first argument is a single integer A.
The second argument is a single integer B.


Output Format

Return the minimum value (A ⊕ X) + (B ⊕ X) that can be achieved for any X.


Example Input

Input 1:-
A = 6
B = 12
Input 2:-
A = 4
B = 9
Example Output
Output 1:-
10
output 2:-
13
Example Explanation
Expanation 1:-
X ^ A + X ^ B = 10 when X = 4
Explanation 2:-
X ^ A + X ^ B = 13 when X = 0
     */
    public static void main(String[] args) {
        int num1 = 6, num2 = 12;
        // 6 ->   0110
        // 12 ->  1100
        // sum -> 1010 -> 2^3 + 2^1 = 10
        System.out.println("XOR Sum is : "+xorSum(num1, num2));
        int num3 = 4, num4 = 9;
        System.out.println("XOR Sum is : "+xorSum(num3, num4));

    }
    private static int xorSum(int num1, int num2){
        return num1 ^ num2;
    }
}
