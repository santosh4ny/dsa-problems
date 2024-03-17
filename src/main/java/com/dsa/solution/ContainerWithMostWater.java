package com.dsa.solution;

public class ContainerWithMostWater {

    public static int maxAreaBruteForce(int[] heights){
        int finalArea =0;
        if(heights.length < 2){
            return finalArea;
        }
        for(int p1=0; p1<heights.length; p1++){
            for(int p2= p1+1; p2<heights.length; p2++){
                int height = Math.min(heights[p1], heights[p2]);
                int weidth = p2 - p1;
                int area = height * weidth;
                finalArea = Math.max(finalArea, area);
            }
        }
        return finalArea;
    }

    public static int maxAreaWithOptimized(int [] heights){
        int finalArea =0;
        if(heights.length < 2){
            return finalArea;
        }
        int left = 0, right = heights.length -1;
        while(left < right){
            int height = Math.min(heights[left], heights[right]);
            int width = right - left;
            int area = height * width;
            finalArea = Math.max(finalArea, area);
            if(heights[left] < heights[right]){
                left ++;
            }else{
                right --;
            }
        }

        return finalArea;
    }

    public static void main(String [] args){

        System.out.println("Container with max water is : "+ maxAreaBruteForce(new int[]{1,8,6,2,5,4,8,3,7}));
        System.out.println("Container with max water is : "+ maxAreaBruteForce(new int[]{1,1}));
        System.out.println("Container with max water optimized is : "+ maxAreaWithOptimized(new int[]{1,8,6,2,5,4,8,3,7}));
        System.out.println("Container with max water optimized is : "+ maxAreaWithOptimized(new int[]{1,1}));
    }

}
