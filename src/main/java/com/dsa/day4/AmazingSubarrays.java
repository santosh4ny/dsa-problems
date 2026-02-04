package com.dsa.day4;

public class AmazingSubarrays {
    /*
You are given a string S, and you have to find all the amazing substrings of S.

An amazing Substring is one that starts with a vowel (a, e, i, o, u, A, E, I, O, U).

Input

Only argument given is string S.
Output

Return a single integer X mod 10003, here X is the number of Amazing Substrings in given the string.
Constraints

1 <= length(S) <= 1e6
S can have special characters
Example

Input
    ABEC

Output
    6

Explanation
    Amazing substrings of given string are :
    1. A
    2. AB
    3. ABE
    4. ABEC
    5. E
    6. EC
    here number of substrings are 6 and 6 % 10003 = 6.
     */
    public static void main(String[] args) {
        String str = "ABEC";
        System.out.println("Count of amazing subarray is : "+countOfAmazingSubarray(str));
    }
    private static int countOfAmazingSubarray(String str){
        int len = str.length();
        int count = 0;
        for(int i=0; i<len; i++){
            if(str.charAt(i) == 'A' || str.charAt(i) == 'a' || str.charAt(i) == 'E' ||
                    str.charAt(i) == 'e' || str.charAt(i) == 'I' || str.charAt(i) == 'i' ||
                    str.charAt(i) == 'O' || str.charAt(i) == 'o' || str.charAt(i) == 'U' ||
                    str.charAt(i) == 'u'){
                count=(count+len-i) % 10003;
            }
        }
        return count;
    }

}
