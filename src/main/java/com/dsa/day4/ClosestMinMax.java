package com.dsa.day4;

public class ClosestMinMax {
    /*
    Given an array A, find the size of the smallest subarray such that
    it contains at least one occurrence of the maximum value of the array
    and at least one occurrence of the minimum value of the array.
    Problem Constraints

    1 <= |A| <= 2000

    Input Format

    First and only argument is vector A

    Output Format

    Return the length of the smallest subarray which has at least one occurrence of
    minimum and maximum element of the array

    Example Input

    Input 1:

    A = [1, 3, 2]
    Input 2:

    A = [2, 6, 1, 6, 9]


    Example Output

    Output 1:

     2
    Output 2:

     3


    Example Explanation

    Explanation 1:

     Take the 1st and 2nd elements as they are the minimum and maximum elements respectievly.
    Explanation 2:

    Take the last 3 elements of the array.
     */
    public static void main(String[] args) {
        int [] arr = {1, 3, 2};
        int [] nums = {2, 6, 1, 6, 9};
        System.out.println("Closest min max is : "+ countClosestMinMax(arr));
        System.out.println("Closest min max is : "+ countClosestMinMax(nums));
    }

    private static int countClosestMinMax(int[] nums){
        int totalCount = Integer.MAX_VALUE;
        int n = nums.length;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for(int i=0; i<n; i++){
            if(nums[i] > max){
                max = nums[i];
            }
            if(nums[i] < min){
                min = nums[i];
            }
        }

        for(int i=0; i<n; i++){
            if(nums[i] == max){
                for(int j=i+1; j<n; j++){
                    if(nums[j] == min){
                        int count = j - i + 1;
                        if(count < totalCount){
                            totalCount = count;
                        }
                    }
                }
            }
            else if(nums[i] == min){
                for(int j=i+1; j<n; j++){
                    if(nums[j] == max){
                        int count = j - i + 1;
                        if(count < totalCount){
                            totalCount = count;
                        }
                    }
                }
            }
        }
        return totalCount;
    }
}
