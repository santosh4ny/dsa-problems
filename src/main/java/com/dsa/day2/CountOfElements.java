package com.dsa.day2;

public class CountOfElements {
    /**
     * Given an array A of N integers.
     * Count the number of elements that have at least 1
     * elements greater than itself.
     */
    public static void main(String[] args) {
        int[] arr = {3, 1, 2};
        System.out.println("Max Count is : "+countOfElement(arr));
        System.out.println("Max Count is : "+countOfElement(new int[]{5, 5, 3}));
        System.out.println("Max Count is : "+countOfElement(new int[]{8, 1, 2, 3, 4, 8, 7, 8}));
    }

    private static int countOfElement(int[] arr){
        // find the max element
        int maxElement = arr[0];
        int count = 0;
        for(int i = 0; i<arr.length; i++){
            if(maxElement < arr[i]){
                maxElement = arr[i];
                count = 1;
            }else if(maxElement == arr[i]){
                count += 1;
            }
        }
        // count the max element

//        for(int element : arr){
//            if(element == maxElement){
//                count += 1;
//            }
//        }
        return arr.length - count;
    }
}
