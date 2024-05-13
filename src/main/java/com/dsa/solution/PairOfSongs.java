package com.dsa.solution;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PairOfSongs {

    public static int pairOfSongBruteForce(int[] times, int duration){
        int count = 0;
        for(int p1=0; p1<times.length; p1++){
            for (int p2=p1+1; p2<times.length; p2++){
                if((times[p1] + times[p2]) % 60 == 0){
                    count++;
                    System.out.println("[ "+p1+", "+p2+" ]");
                }
            }
        }
        return count;
    }
    public static int pairOfSongsOptimized(int[] times, int duration){
        int count = 0;
        int [] moduloOfTimes = new int[times.length];
        Map<Integer, Integer> moduloOfMaps = new HashMap<>();
        for(int p=0; p<times.length; p++){
            moduloOfTimes[p] = times[p] % duration;
            moduloOfMaps.put(times[p] % duration, p);
        }
        System.out.println(moduloOfTimes.toString());
        Arrays.stream(moduloOfTimes).forEach(System.out::println);
        for(int i=0; i< times.length; i++){
            int target = duration - moduloOfTimes[i];
//            if()
        }

        return count;
    }
    public static void main(String[] args) {
        int [] times = new int[]{30, 20, 150, 100, 40};
        int duration = 60;
        System.out.println("Count of Pair of Songs: "+pairOfSongBruteForce(times, duration));
        pairOfSongsOptimized(times, duration);
    }
}
