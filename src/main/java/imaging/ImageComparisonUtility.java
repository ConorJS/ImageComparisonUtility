package imaging;

import filehandling.FileHandlerUtil;
import filehandling.HashCacheManager;
import imaging.sampler.FingerprintConfig;
import imaging.sampler.Sampler;
import imaging.scoring.NumberOrderedPairList;
import imaging.threading.ImageLoaderWorker;
import imaging.util.SimpleColor;
import imaging.util.SimplePair;
import main.ApplicationConfig;
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
import java.util.List;

public class ImageComparisonUtility {

    private final FingerprintConfig fingerprintConfig = new FingerprintConfig(10, 10, 25);
    private ProgressBarFeedbackProxy progressBarFeedbackProxy;
    
    public void runApp() {

        UI ui = new UI(this);

        this.progressBarFeedbackProxy = new ProgressBarFeedbackProxy(ui);

        ui.showUI();
    }

    public List<SimplePair<SimplePair<String, String>, Double>> runImageComparisonForPath(String path) {
        HashCacheManager hashCacheManager = new HashCacheManager(path);

        // Load the images into picture samplers
        List<Sampler> pictureSamplers = loadImages(hashCacheManager, path);

        // Each pair key is a pair containing both file names the comparison was drawn between,
        // the value is the comparison score
        List<SimplePair<SimplePair<String, String>, Double>> duplicatePairs = new ArrayList<>();
        for (int i = 0; i < pictureSamplers.size(); i++) {

            Sampler subject = pictureSamplers.get(i);

            NumberOrderedPairList<String> comparisonScores = getAllComparisonScoresForImage(subject, pictureSamplers);

            duplicatePairs.addAll(getDuplicates(comparisonScores, subject));
        }

        trimBiDirectionalPairings(duplicatePairs);

        // DEBUG
        System.out.println("# duplicate pairs: " + duplicatePairs.size());
        for (SimplePair<SimplePair<String, String>, Double> duplicatePair : duplicatePairs) {
            System.out.println(duplicatePair.getKey().getKey() + "," +
                    duplicatePair.getKey().getValue() + "," +
                    duplicatePair.getValue());
        }

        return duplicatePairs;
    }

    private List<Sampler> loadImages(HashCacheManager hashCacheManager, String path) {

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

                    if (hashCacheManager.isCached(hash, fingerprintConfig)) {
                        // Cache hit
                        Sampler loadedFromCache = hashCacheManager.loadCachedSampler(hash);

                        // Make sure we associate the new file name with the cached fingerprint;
                        // file names change, hashes tend not to unless the file was modified.
                        loadedFromCache.setFile(picture);
                        loadedFromCache.setFileMdHash(hash);
                        pictureSamplers.add(loadedFromCache);

                        this.progressBarFeedbackProxy.incrementProgressBar();

                    } else {
                        // Cache miss, pass in the hash as we've already calculated it
                        System.out.println("Cache miss for " + picture.getName() + ", calculating fingerprint.");
                        ImageLoaderWorker imageLoaderWorker =
                                new ImageLoaderWorker( (SynArrayList<Sampler>) pictureSamplers, picture, fingerprintConfig,
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
            hashCacheManager.cache(sampler);
        }
        hashCacheManager.saveCache();

        return pictureSamplers;
    }

    private NumberOrderedPairList<String> getAllComparisonScoresForImage(Sampler subject, List<Sampler> comparisonTargets) {

        NumberOrderedPairList<String> comparisonScores = new NumberOrderedPairList<>();

        for (Sampler comparisonTarget : comparisonTargets) {

            if (subject != comparisonTarget) {
                if (comparisonTarget.getFileMdHash() == subject.getFileMdHash()) {

                    // Handle hash matches differently: a 0 difference score means hash-identical
                    // (this is impossible to achieve otherwise, even when comparing identical files)
                    comparisonScores.add(comparisonTarget.getFile().getName(), 0);

                } else {

                    int comparisonScore = getComparisonScore(
                            subject.getFingerprint(this.fingerprintConfig), subject.getNoiseScore(),
                            comparisonTarget.getFingerprint(this.fingerprintConfig), comparisonTarget.getNoiseScore());

                    comparisonScores.add(comparisonTarget.getFile().getName(), comparisonScore);
                }
            }
        }

        return comparisonScores;
    }

    private List<SimplePair<SimplePair<String, String>, Double>> getDuplicates(
            NumberOrderedPairList<String> comparisonScores, Sampler leftSampler) {

        List<SimplePair<SimplePair<String, String>, Double>> duplicatePairs = new ArrayList<>();

        // compare against to get an idea of how divergent the lowest score is WRT the nth score
        double nthValue = comparisonScores.getNthSmallest(ApplicationConfig.EXPECT_MAX_DUPLICATES).getValue();

        for (SimplePair<String, Integer> comparisonScore : comparisonScores.toList()) {

            double diff = comparisonScore.getValue();
            double divergenceRatio = (nthValue / diff);

            if (divergenceRatio > ApplicationConfig.DIVERGENCE_TOLERANCE_FACTOR) {

                // TODO: Probably refactor away SimplePair/SimpleTriple usage and use bespoke classes
                duplicatePairs.add(new SimplePair<>(
                        new SimplePair<>(comparisonScore.getKey(), leftSampler.getFile().getName()),
                        diff));
            }
        }

        return duplicatePairs;
    }

    private void trimBiDirectionalPairings(List<SimplePair<SimplePair<String, String>, Double>> duplicatePairs) {

        // trim bi-directional pairings
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

    // TODO: This appears to work without adding weighting parameters, thorough testing could improve this though
    private int adjustScoreForNoise(int score, double leftNoiseScore, double rightNoiseScore) {

        // We need to adjust our tolerance significantly when
        // comparing two images that both have very low noise
        double relativeNoiseFactor = 1.0 / Math.sqrt(Math.max(leftNoiseScore, rightNoiseScore));

        return (int)(score * relativeNoiseFactor);
    }
}
