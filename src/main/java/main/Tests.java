package main;

import imaging.scoring.ImageNoiseScorer;
import imaging.sampler.Sampler;

import java.io.File;

// TODO: Move these to JUnit tests
public class Tests {

    public static void testImageNoiseScores() {
        File file1 = new File("TestFileVeryHigh.jpg");
        File file2 = new File("TestFileHigh.jpg");
        File file3 = new File("TestFileTypical.jpg");
        File file4 = new File("TestFileLow.jpg");

        Sampler sampler1 = new Sampler(file1);
        Sampler sampler2 = new Sampler(file2);
        Sampler sampler3 = new Sampler(file3);
        Sampler sampler4 = new Sampler(file4);

        System.out.println("Scores:\n" +
                "VERY HIGH: " + ImageNoiseScorer.getImageNoiseScore(sampler1) + "\n" +
                "HIGH: " + ImageNoiseScorer.getImageNoiseScore(sampler2) + "\n" +
                "TYPICAL: " + ImageNoiseScorer.getImageNoiseScore(sampler3) + "\n" +
                "LOW: " + ImageNoiseScorer.getImageNoiseScore(sampler4));

    }

}
