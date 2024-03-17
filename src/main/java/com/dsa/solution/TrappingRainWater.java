package com.dsa.solution;

public class TrappingRainWater {

    public static int trapWithBrute(int [] heights){
        int totalWater = 0;
        for(int p=0; p<heights.length; p++){
            int leftP = p, rightP = p, maxLeft =0, maxRight =0;
            while(leftP >= 0){
                maxLeft = Math.max(maxLeft, heights[leftP]);
                leftP--;
            }
            while(rightP < heights.length){
                maxRight = Math.max(maxRight, heights[rightP]);
                rightP++;
            }
            int currentWater = Math.min(maxLeft, maxRight) - heights[p];
            if(currentWater >=0){
                totalWater += currentWater;
            }
        }
        return totalWater;
    }

    public static int trapWithOptimized(int [] heights){
        int maxLeft=0, maxRight=0,totalWater = 0, left =0, right = heights.length -1;
        while(left < right){
            if(heights[left] <= heights[right]){
                if(heights[left] >= maxLeft){
                    maxLeft = heights[left];
                }else{
                    totalWater += maxLeft - heights[left];
                }
                left++;
            }else{
                if(heights[right] >= maxRight){
                    maxRight = heights[right];
                }else {
                    totalWater += maxRight - heights[right];
                }
                right --;
            }
        }

        return totalWater;
    }

    public static void main(String[] args) {
        System.out.println("Trap Rain Water with Brute solution: "+ trapWithBrute(new int[]{0, 1, 0, 2, 1, 0, 3, 1, 0, 1, 2}));
        System.out.println("Trap Rain Water with Brute solution: "+ trapWithBrute(new int[]{4,2,0,3,2,5}));
        System.out.println("Trap Rain Water with optimized solution: "+ trapWithOptimized(new int[]{0, 1, 0, 2, 1, 0, 3, 1, 0, 1, 2}));
        System.out.println("Trap Rain Water with optimized solution: "+ trapWithOptimized(new int[]{4,2,0,3,2,5}));
    }
}
