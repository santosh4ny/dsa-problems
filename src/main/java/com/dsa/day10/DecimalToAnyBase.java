package com.dsa.day10;

public class DecimalToAnyBase {
    /*
Problem Description

Given a decimal number A and a base B, convert it into its equivalent number in base B.


Problem Constraints

0 <= A <= 512
2 <= B <= 10


Input Format

The first argument will be decimal number A.
The second argument will be base B.


Output Format

Return the conversion of A in base B.


Example Input

Input 1:
A = 4
B = 3
Input 2:
A = 4
B = 2
Example Output
Output 1:
11
Output 2:
100


Example Explanation

Explanation 1:
Decimal number 4 in base 3 is 11.
Explanation 2:
Decimal number 4 in base 2 is 100.
     */
    public static void main(String[] args) {
        int decimal = 4;
        int base = 3;
        System.out.println("Decimal to Any Base : "+decimalToBase(decimal, base));
        int decimal1 = 4;
        int base1 = 2;
        System.out.println("Decimal to Any Base 1 : "+decimalToBase(decimal1, base1));
        int decimal2 = 14;
        int base2 = 2;
        System.out.println("Decimal to Any Base 2 : "+decimalToBase(decimal2, base2));
    }
    private static int decimalToBase(int decimal, int base){
        String str = "";
        if(decimal == 0){
            return 0;
        }
        while(decimal != 0){
            str += decimal % base;
            decimal /= base;
        }
        // reverse the string so i can get the correct answer
        String newStr = "";
        for(int i=str.length()-1; i>=0; i--){
            newStr += str.charAt(i);
        }
        return Integer.parseInt(newStr);
    }
}
