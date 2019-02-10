package imaging;

import filehandling.FileHandlerUtil;
import filehandling.HashCacheManager;
import imaging.sampler.Sampler;
import imaging.sampler.SamplerConfig;
import imaging.threading.ImageLoaderWorker;
import imaging.threading.NumberOrderedPairList;
import imaging.util.SimpleColor;
import imaging.util.SimplePair;
import threading.EventTimer;
import threading.SynArrayList;
import threading.ThreadPool;
import ui.ProgressBarFeedbackProxy;
import ui.UI;

import java.awt.*;
import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageComparisonUtility {

    private final SamplerConfig samplerConfig = new SamplerConfig(10, 10, 25);

    private static final double DIVERGENCE_TOLERANCE_FACTOR = 1.8;
    private static final int EXPECT_MAX_DUPLICATES = 4;

    private ProgressBarFeedbackProxy progressBarFeedbackProxy;
    
    public void runApp() {

        UI ui = new UI(this);

        this.progressBarFeedbackProxy = new ProgressBarFeedbackProxy(ui);

        ui.showUI();
    }

    // TODO: Break down this method
    public List<SimplePair<SimplePair<String, String>, Double>> runImageComparisonForPath(String path) {
        HashCacheManager hashCacheManager = new HashCacheManager(path);
        ThreadPool threadPool = new ThreadPool();

        // Get all images, create samplers
        List<File> pictures = FileHandlerUtil.getAllFiles(path, false);
        List<Sampler> pictureSamplers = new SynArrayList<>();

        for (File picture : pictures) {

            // Simplify nested ifs when this all works
            if ((!picture.getPath().endsWith(".au3")) && (!picture.isDirectory()) && (!picture.getPath().endsWith(".7z"))) {

                try {
                    InputStream is = new FileInputStream(picture);
                    int hash = hashFile(is);

                    if (hashCacheManager.isCached(hash, samplerConfig)) {
                        // Cache hit
                        Sampler loadedFromCache = hashCacheManager.loadCachedSampler(hash);

                        // Make sure we associate the new file name with the cached fingerprint;
                        // filenames change, hashes tend not to unless the file was modified.
                        loadedFromCache.setFile(picture);
                        loadedFromCache.setFileMdHash(hash);
                        pictureSamplers.add(loadedFromCache);

                        this.progressBarFeedbackProxy.incrementProgressBar();

                    } else {
                        // Cache miss, pass in the hash as we've already calculated it
                        System.out.println("Cache miss for " + picture.getName() + ", calculating fingerprint.");
                        ImageLoaderWorker imageLoaderWorker =
                                new ImageLoaderWorker( (SynArrayList<Sampler>) pictureSamplers, picture, samplerConfig,
                                        this.progressBarFeedbackProxy, hash);

                        threadPool.assignWorker(imageLoaderWorker);
                    }
                } catch (FileNotFoundException e) {
                    throw new RuntimeException("Couldn't load file " + picture.getName());
                }

            } else {
                this.progressBarFeedbackProxy.incrementProgressBar();
            }
        }

        threadPool.notifyAllTasksGiven();

        System.out.println("Waiting for threads to finish loading images");
        EventTimer et = new EventTimer();

        while (threadPool.threadsRunning()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("All images loaded in " + et.endTimer() + " ms");

        // Re-cache Samplers
        for (Sampler sampler : pictureSamplers) {
            // TODO: Re-populate cache
            hashCacheManager.cache(sampler);
        }
        hashCacheManager.saveCache();

        // Each pair key is a pair containing both filenames the comparison was drawn between,
        // the value is the comparison score
        // TODO - refactor/remove?
        Map<String, List<SimplePair<String, Integer>>> allComparisonScores = new HashMap<>(); // debug
        List<SimplePair<SimplePair<String, String>, Double>> duplicatePairs = new ArrayList<>();
        for (int i = 0; i < pictureSamplers.size(); i++) {
            NumberOrderedPairList<String> comparisonScores = new NumberOrderedPairList<>();
            Sampler leftSampler = pictureSamplers.get(i);

            for (Sampler rightSampler : pictureSamplers) {

                if (leftSampler != rightSampler) {

                    if (rightSampler.getFileMdHash() == leftSampler.getFileMdHash()) {

                        // Handle hash matches differently: a 0 difference score means hash-identical
                        // (this is impossible to achieve otherwise, even when comparing identical files)
                        comparisonScores.add(rightSampler.getFile().getName(), 0);

                    } else {
                        int comparisonScore = getComparisonScore(
                                leftSampler.getFingerprint(this.samplerConfig), leftSampler.getNoiseScore(),
                                rightSampler.getFingerprint(this.samplerConfig), rightSampler.getNoiseScore());

                        comparisonScores.add(rightSampler.getFile().getName(), comparisonScore);
                    }
                }
            }

            // compare against to get an idea of how divergent the lowest score is WRT the nth score
            double nthValue = comparisonScores.getNthSmallest(EXPECT_MAX_DUPLICATES).getValue();

            for (SimplePair<String, Integer> comparisonScore : comparisonScores.toList()) {

                double diff = comparisonScore.getValue();
                double divergenceRatio = (nthValue / diff);

                if (divergenceRatio > DIVERGENCE_TOLERANCE_FACTOR) {

                    // TODO: Probably refactor away SimplePair/SimpleTriple usage and use bespoke classes
                    duplicatePairs.add(
                            new SimplePair(
                                new SimplePair(
                                        // name of subject image file (LHS)
                                        comparisonScore.getKey(),
                                        // name of subject image file (RHS)
                                        leftSampler.getFile().getName()),
                                diff)
                            );
                }
            }

            allComparisonScores.put(pictureSamplers.get(i).getFile().getName(), comparisonScores.toList()); // debug
        }

        // trim pairings
        // (e.g. omit one pairing in case of two alternating pairs: 123->duplicates->456 and 456->duplicates->123)
        boolean fullPass = false;
        while (!fullPass) {

            fullPass = true;

            for (int i = 0; i < duplicatePairs.size(); i++) {
                if (fullPass) {

                    String filename1 = duplicatePairs.get(i).getKey().getKey();
                    String filename2 = duplicatePairs.get(i).getKey().getValue();

                    for (int j = 0; j < duplicatePairs.size(); j++) {
                        if (fullPass) {

                            String filenameInner1 = duplicatePairs.get(j).getKey().getKey();
                            String filenameInner2 = duplicatePairs.get(j).getKey().getValue();

                            if (filenameInner1.equals(filename2) && (filenameInner2.equals(filename1))) {
                                duplicatePairs.remove(duplicatePairs.get(j));

                                fullPass = false;
                            }
                        }
                    }
                }
            }
        }

        System.out.println("# duplicate pairs: " + duplicatePairs.size());
        // TODO: CSV-like output, doesn't handle files with , characters in them though
        for (int i = 0; i < duplicatePairs.size(); i++) {
            System.out.println(duplicatePairs.get(i).getKey().getKey() + "," +
                    duplicatePairs.get(i).getKey().getValue() + "," +
                    duplicatePairs.get(i).getValue());
        }

        File noiseScoresFile = new File("NoiseScores.csv");
        try {
            FileHandlerUtil.writeLinesToFile(noiseScoresFile, ImageNoiseScorer.NOISE_SCORES);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return duplicatePairs;
    }

    private static int hashFile(InputStream is) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestInputStream dis = new DigestInputStream(is, md);
            md.digest();
            try {
                return dis.available();

            } catch (IOException e) {
                throw new RuntimeException("Couldn't get MD5 hash from file.");
            }

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Couldn't get MD5 algorithm for file hashing.");
        }
    }

    private int getComparisonScore(List<SimplePair<Point, SimpleColor>> leftFingerprint, double leftNoiseScore,
                                   List<SimplePair<Point, SimpleColor>> rightFingerprint, double rightNoiseScore) {
        if (leftFingerprint.size() == rightFingerprint.size()) {
            // Do a simple comparison of pixel colors between two fingerprints
            int score = getDifferenceScore(leftFingerprint, rightFingerprint);

            // Now that we have the difference score, adjust for noise.
            score = adjustScoreForNoise(score, leftNoiseScore, rightNoiseScore);

            return score;

        } else {
            System.out.println("fingerprints not same size");

            throw new RuntimeException("Fingerprints not same size");
        }
    }

    // TODO: Look into reducing the complexity of the parameters to this method
    private int getDifferenceScore(List<SimplePair<Point, SimpleColor>> a, List<SimplePair<Point, SimpleColor>> b) {
        int differenceScore = 0;

        for (int i = 0; i < a.size(); i++) {
            int redDiff = Math.abs(a.get(i).getValue().getRed() - b.get(i).getValue().getRed());
            int greenDiff = Math.abs(a.get(i).getValue().getGreen() - b.get(i).getValue().getGreen());
            int blueDiff = Math.abs(a.get(i).getValue().getBlue() - b.get(i).getValue().getBlue());

            int diff = redDiff + greenDiff + blueDiff;
            differenceScore += diff;
        }

        return differenceScore;
    }

    private int adjustScoreForNoise(int score, double leftNoiseScore, double rightNoiseScore) {
        // We need to adjust our tolerance significantly when
        // comparing two images that both have very low noise
        double relativeNoiseFactor = 1.0 / Math.sqrt(Math.max(leftNoiseScore, rightNoiseScore));
        return (int)(score * relativeNoiseFactor);
    }
}
