package main;

import imaging.ImageComparisonUtility;
import threading.EventTimer;

public class Main {

    private String whatever;

    public static void main(String[] args) {

        EventTimer eventTimerProgram = new EventTimer();

        ImageComparisonUtility imageComparisonUtility = new ImageComparisonUtility();
        imageComparisonUtility.runApp();

        System.out.println("Program ran for: " + eventTimerProgram.endTimer() + " ms");
    }
}
