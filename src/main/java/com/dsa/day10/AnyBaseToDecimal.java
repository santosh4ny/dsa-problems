package com.dsa.day10;

public class AnyBaseToDecimal {
    /*
Problem Description

You are given a number A. You are also given a base B. A is a number on base B.
You are required to convert the number A into its corresponding value in decimal number system.


Problem Constraints

0 <= A <= 109
2 <= B <= 9


Input Format

First argument A is an integer.
Second argument B is an integer.


Output Format

Return an integer.


Example Input

Input 1:
A = 1010
B = 2
Input 2:
A = 22
B = 3
Example Output

Output 1:
10
Output 2:
8


Example Explanation

For Input 1:
The decimal 10 in base 2 is 1010.
For Input 2:
The decimal 8 in base 3 is 22.
     */
    public static void main(String[] args) {
        int num = 1010;
        int base = 2;
        System.out.println("Decimal Value is : "+baseToDecimal(num, base));
        int num1 = 22;
        int base1 = 3;
        System.out.println("Decimal Value is 1 : "+baseToDecimal(num1, base1));
        int num2 = 14;
        int base2 = 6;
        System.out.println("Decimal Value is 2 : "+baseToDecimal(num2, base2));
    }
    private static int baseToDecimal(int num, int base){
        String str = ""+ num;
        int power = str.length() -1;
        int start = 0;
        int result = 0;
        while(power >= 0){
            int value = Character.getNumericValue(str.charAt(start));
            int pow = (int) Math.pow(base, power);
            result += value * pow;
            power--;
            start++;
        }
        return result;
    }
}
