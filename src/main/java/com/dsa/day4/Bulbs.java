package com.dsa.day4;

public class Bulbs {
    /*
Problem Description

A wire connects N light bulbs.


Each bulb has a switch associated with it; however, due to faulty wiring,
a switch also changes the state of all the bulbs to the right of the current bulb.
Given an initial state of all bulbs, find the minimum number of switches you have
to press to turn on all the bulbs.
You can press the same switch multiple times.

Note: 0 represents the bulb is off and 1 represents the bulb is on.




Problem Constraints

0 <= N <= 5×105
0 <= A[i] <= 1
Input Format

The first and the only argument contains an integer array A, of size N.
Output Format
Return an integer representing the minimum number of switches required.
Example Input
Input 1:

 A = [0, 1, 0, 1]
Input 2:

 A = [1, 1, 1, 1]


Example Output

Output 1:

 4
Output 2:

 0


Example Explanation

Explanation 1:

 press switch 0 : [1 0 1 0]
 press switch 1 : [1 1 0 1]
 press switch 2 : [1 1 1 0]
 press switch 3 : [1 1 1 1]
Explanation 2:

 There is no need to turn any switches as all the bulbs are already on.
     */
    public static void main(String[] args) {
        int [] switches = {0, 1, 0, 1};
        int [] switches1 = {1, 1, 1, 1};
        System.out.println("No of Step required to switched on is : "+ bulb(switches));
        System.out.println("No of Step required to switched on is : "+ bulb(switches1));
    }
    private static int bulb(int [] switches){
        boolean flipped = false;
        int ans = 0;
        for(int i=0; i<switches.length; i++){
            if(flipped == false){
                if(switches[i] == 0){
                    ans++;
                    flipped = true;
                }
            } else if(flipped == true){
                if(switches[i] == 1){
                    ans++;
                    flipped = false;
                }
            }
        }
        return ans;
    }
}
