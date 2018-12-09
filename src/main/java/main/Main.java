package main;

import imaging.ImageComparisonUtility;
import threading.EventTimer;

public class Main {

    private String whatever;

    public static void main(String[] args) {

        EventTimer eventTimerProgram = new EventTimer();

        ImageComparisonUtility imageComparisonUtility = new ImageComparisonUtility();
        imageComparisonUtility.runApp();

//        PixelArtRestorer pixelArtRestorer =
//                new PixelArtRestorer("mario.jpg", "RestoredPixelArt.bmp");
//        System.out.println("Block size is likely: " + pixelArtRestorer.determineBlockSize());

        System.out.println("Program ran for: " + eventTimerProgram.endTimer() + " ms");

    }

}
