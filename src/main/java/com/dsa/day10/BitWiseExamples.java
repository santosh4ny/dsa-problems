package com.dsa.day10;

public class BitWiseExamples {
    public static void main(String[] args) {
        int [] arr = { 1, 2, 3, 4, 3, 2, 1};
        System.out.println("Different element is : "+ findDifferentElement(arr));
        System.out.println(solve(14, 6));
    }
    private static int findDifferentElement(int[] arr){
        int ans = 0;
        for(int i=0; i<arr.length; i++){
            ans = ans ^ arr[i];
        }
        return ans;
    }

    public static int solve(int A, int B) {
        String s = "";
        s = s + A;
        int result = 0;
        int last = s.length()-1;
        char[] ch = s.toCharArray();
        int i=0;
        while(last >= 0){
            int power = (int) Math.pow(B, last);
            result = result + (Character.getNumericValue(ch[i]) * power) ;
            last--;
            i++;
        }
        return result;
    }
}
