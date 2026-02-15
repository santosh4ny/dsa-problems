package com.dsa.day8;

public class LengthOfLongestConsecutiveOnes {
    /*
Given a binary string A. It is allowed to do at most one swap between any 0 and 1.
Find and return the length of the longest consecutive 1’s that can be achieved.
Input Format
The only argument given is string A.
Output Format
Return the length of the longest consecutive 1’s that can be achieved.
Constraints
1 <= length of string <= 1000000
A contains only characters 0 and 1.
For Example
Input 1:
    A = "111000"
Output 1:
    3
Input 2:
    A = "111011101"
Output 2:
    7
     */
    public static void main(String[] args) {
        String s = "111000";
        String s1 = "111011101";
        System.out.println("Max Ones : "+ countOfConsecutiveOnes(s));
        System.out.println("Max Ones : "+ countOfConsecutiveOnes(s1));
    }
    private static int countOfConsecutiveOnes(String str){
        char[] ch = str.toCharArray();
        int maxOnes = 0;
        int countOne = 0;
        for(int i=0; i<ch.length; i++){
            if(ch[i] == '1'){
                countOne++;
            }
        }
        for(int i=0; i<ch.length; i++){
            if(ch[i] == '0'){
                int left = 0;
                for(int j=0; j < i; j++){
                    if(ch[j] == '1'){
                        left += 1;
                    }else{
                        break;
                    }
                }
                int right = 0;
                for(int k = i+1; k<ch.length; k++){
                    if(ch[k] == '1'){
                        right += 1;
                    }else{
                        break;
                    }
                }
                if(countOne > left + right){
                    maxOnes = Math.max(maxOnes, left + right + 1);
                }else{
                    maxOnes = Math.max(maxOnes, left + right);
                }

            }
        }
        return maxOnes;
    }

}
