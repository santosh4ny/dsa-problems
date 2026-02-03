package com.dsa.day3;

public class EquilibriumIndices {
    /**
     * Given N array elements, count number of equilibrium indices.
     * Equilibrium : An indices is said to be an equilibrium index
     * if sum of left side equal to sum of right side.
     *
     */
    public static void main(String[] args) {
        int [] arr = {-7, 1, 5, 2, -4, 3,0};
        System.out.println("Number of equilibrium index is : "+equilibriumIndex(arr));
        int [] arr1 = {-3, 2, 4, -1};
        System.out.println("Number of equilibrium index is : "+equilibriumIndex(arr1));
    }
    private static int equilibriumIndex(int [] arr){
        int[] pf = new int[arr.length];
        int count = 0;
        //1. create a pf sum for all the array
        pf[0] = arr[0];
        for(int i=1; i<arr.length; i++){
            pf[i] = pf[i-1] + arr[i];
        }
        //2. iterate the array if index is less than 0 or greater than size of array
        // mark sum as 0 form either side
        for(int i = 0; i<arr.length; i++){
            int leftSum = 0, rightSum = 0;
            int left = 0;
            int right = arr.length -1;
            if(left != 0){
                leftSum = pf[i-1];
            }
            rightSum = pf[right] - pf[i];
            if(leftSum == rightSum){
                count++;
            }
        }
        return count;
    }
}
