package com.dsa.day4;

// I am using here Carry forward approach
public class SpecialSubSequencesAG {
    /*
    You have given a string A having Uppercase English letters.

You have to find how many times subsequence "AG" is there in the given string.

NOTE: Return the answer modulo 109 + 7 as the answer can be very large.



Problem Constraints

1 <= length(A) <= 105



Input Format

First and only argument is a string A.



Output Format

Return an integer denoting the answer.



Example Input

Input 1:

 A = "ABCGAG"
Input 2:

 A = "GAB"


Example Output

Output 1:

 3
Output 2:

 0


Example Explanation

Explanation 1:

 Subsequence "AG" is 3 times in given string
Explanation 2:

 There is no subsequence "AG" in the given string.
     */
    public static void main(String[] args) {
        String str = "ABCGAG";
        System.out.println("Total count of AG is : "+countAg(str));
        String str1 = "GAB";
        System.out.println("Total count of AG is : "+countAg(str1));
    }
    private static int countAg(String str){
        int n = str.length();
        int count = 0, totalCount = 0;
        for(int i=n-1; i>=0; i--){
            if(str.charAt(i) == 'G'){
                count++;
            }
            if(str.charAt(i) == 'A'){
                totalCount += count;
            }
        }

        return totalCount;
    }
}
