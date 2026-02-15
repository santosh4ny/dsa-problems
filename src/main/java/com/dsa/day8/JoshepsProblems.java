package com.dsa.day8;

public class JoshepsProblems {
    /*
Problem Description

There are A people standing in a circle.
Person 1 kills their immediate clockwise neighbour and
pass the knife to the next person standing in circle.
This process continues till there is only 1 person remaining.
Find the last person standing in the circle.
Problem Constraints
1 <= A <= 105
Input Format
First argument A is an integer.
Output Format
Return an integer.
Example Input
Input 1:
A = 4
Input 2:
A = 5
Example Output
Output 1:
1
Output 2:
3
Example Explanation
For Input 1:
Firstly, the person at position 2 is killed, then the person at position 4 is killed,
then the person at position 3 is killed. So the person at position 1 survives.
For Input 2:

Firstly, the person at position 2 is killed, then the person at position 4 is killed,
then the person at position 1 is killed. Finally, the person at position 5 is killed.
So the person at position 3 survives.
     */
    public static void main(String[] args) {
        int input = 12;
        System.out.println(josephProblem2(input));
        int input1 = 5;
        System.out.println(josephProblem2(input1));
        int input2 = 7;
        System.out.println(josephProblem2(input2));
    }
    private static int josephProblem2(int number){
        int current = 1;
        int previous = 0;
        while(current <= number){
            previous = current;
            current *= 2;
        }
        int position = number - previous;
        return 2 * position + 1;
    }
}
