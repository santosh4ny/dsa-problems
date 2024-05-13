package com.dsa.scaler.revision.bitmanipulation;

public class SingleElement {


    public static void main(String[] args) {
        int[] array = {6, 9, 10, 9, 6};
        int[] array1 = {12, 9, 8, 7, 7, 8, 9, 12};
        System.out.println("Uniqu Number is : "+getUniqueElement(array));
        System.out.println("Uniqu Number is : "+getUniqueElement(array1));

        int[] nums = {2, 2, 3, 2, 3, 3, 1}; // Example array
        System.out.println("The unique element is: " + getUniqueElementWithThriceOccurance(nums));
        int[] nums1 = {5, 7, 5, 9, 7, 11, 11, 7, 5, 11}; // Example array
        System.out.println("The unique element is: " + getUniqueElementWithThriceOccurance(nums1));


        System.out.println(" ==============================");
        isBitSetOrNotUsingLeftShift(45, 1);
        isBitSetOrNotUsingRightShift(45, 1);

        System.out.println("====================================");

        int num1 = 5; // 101 in binary
        int num2 = 3; // 011 in binary

        // Bitwise AND
        int resultAnd = num1 & num2; // 001 (1 in binary)
        System.out.println("Bitwise AND: " + resultAnd);

        // Bitwise OR
        int resultOr = num1 | num2; // 111 (7 in binary)
        System.out.println("Bitwise OR: " + resultOr);

        // Bitwise XOR
        int resultXor = num1 ^ num2; // 110 (6 in binary)
        System.out.println("Bitwise XOR: " + resultXor);

        // Bitwise NOT
        int resultNot = ~num1; // 11111111111111111111111111111010 (-6 in binary, due to two's complement representation)
        System.out.println("Bitwise NOT: " + resultNot);

        // Left Shift
        int resultLeftShift = num1 << 1; // 1010 (10 in binary)
        System.out.println("Left Shift: " + resultLeftShift);

        // Right Shift
        int resultRightShift = num1 >> 1; // 10 (2 in binary)
        System.out.println("Right Shift: " + resultRightShift);
    }

    /*
    Given n array elements.
    Everything is repeated twice except one element.
    find unique element.
     */
    private static int getUniqueElement(int[] array){
        int ans = 0;

        for(int i=0; i < array.length; i++){
            ans = ans ^ array[i];
        }

        return ans;
    }
    private static int getUniqueElementWithThriceOccurance(int [] nums){
        // Count array to store occurrences of each bit position
        int [] count = new int [32];
        // Iterate through each element in the array
        for(int num : nums){
            // Iterate through each bit position
            for(int i=0; i< 32; i++){
                // Count occurrences of each bit position
                count[i] += (num >> i) &1;
            }
        }
        int unique =0;
        // Construct the unique number by setting the bit positions with counts not divisible by 3
        for(int j=0; j<32; j++){
            if(count[j] % 3 != 0){
                unique = unique | (1 << j);
            }
        }
        return unique;
    }
    /*
    Given a number check if its ith bit is set or not.
     */

    private static void isBitSetOrNotUsingRightShift(int number, int i){
        if(((number >> i ) & 1) == 1){
            System.out.println("Using Right shift Bit is set at index : "+i);
        }else {
            System.out.println("Using Right shift Bit is not set at index : "+i);
        }

    }
    private static void isBitSetOrNotUsingLeftShift(int number, int index){
        if(((number << index) &1) == 0){
            System.out.println("Using Left shift Bit is set at index : "+index);
        }else {
            System.out.println("Using Left shift Bit is not set at index : "+index);
        }
    }
}
