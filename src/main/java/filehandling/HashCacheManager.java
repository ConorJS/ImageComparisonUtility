package filehandling;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import imaging.Sampler;
import imaging.SamplerConfig;
import imaging.SimpleColor;
import imaging.SimplePair;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class HashCacheManager {

    private static final String CACHE_DIRECTORY = ".duplicate_detection";
    private static final String CACHE_FILE = "cache";
    private static final String CACHE_PATH = CACHE_DIRECTORY + "\\" + CACHE_FILE;

    private final JsonNodeFactory jsonNodeFactory = new JsonNodeFactory(false);

    private Map<Integer, Sampler> cache;
    private final Map<Integer, Sampler> newCache = new HashMap<>();

    private final String cacheDirectoryAbsolutePath;
    private final String absoluteCachePath;

    public HashCacheManager(String imageFolderPath) {
        cacheDirectoryAbsolutePath = imageFolderPath + "/" + CACHE_DIRECTORY;
        absoluteCachePath = imageFolderPath + "/" + CACHE_PATH;
        loadCache();

        ObjectMapper mapper = new ObjectMapper();

        File cacheFile = new File(absoluteCachePath);
        if (cacheFile.exists()) {
            try {
                mapper.readTree(cacheFile);

            } catch (IOException e) {
                throw new RuntimeException("Couldn't read cache: " + absoluteCachePath + ". Maybe it is corrupt?");
            }
        } else {
            System.out.println("No cache detected, creating " + absoluteCachePath);
        }
    }

    public void cache(Sampler sampler) {
        newCache.put(sampler.getFileMdHash(), sampler);
    }

    public void saveCache() {
        File cacheFile = new File(absoluteCachePath);
        try {
            File directory = new File(cacheDirectoryAbsolutePath);
            if (!directory.exists() && !directory.mkdir()) {
                throw new RuntimeException("Couldn't create cache directory.");
            }
            ObjectMapper mapper = new ObjectMapper();
            ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
            writer.writeValue(cacheFile, hashCacheAsJson(newCache.values()));
            System.out.println("Saved cache at: " + cacheFile.getAbsolutePath());

        } catch (IOException e) {
            throw new RuntimeException("Couldn't write cache: " + absoluteCachePath);
        }
    }

    // Cache hit if cache exists, a file with the same hash was fingerprinted + cached,
    // and that the cached fingerprint was performed with the same configuration as what is being requested
    public boolean isCached(int imageFileHash, SamplerConfig config) {
        return (cache != null) &&
                cache.containsKey(imageFileHash) &&
                cache.get(imageFileHash).getSamplerConfig_cached().equals(config);
    }

    public Sampler loadCachedSampler(Integer imageFileHash) {
        // Returns a copy of a sampler, we don't want two files
        // sharing the same hash to point to the same Sampler object
        return cache.get(imageFileHash).copy();
    }

    private JsonNode hashCacheAsJson(Collection<Sampler> samplers) {
        ArrayNode cache = jsonNodeFactory.arrayNode();
        for (Sampler sampler : samplers) {
            cache.add(samplerAsJson(sampler));
        }

        return cache;
    }

    private JsonNode samplerAsJson(Sampler sampler) {
        ObjectMapper mapper = new ObjectMapper();

        // Root
        ObjectNode rootNode = jsonNodeFactory.objectNode();

        // Hash
        NumericNode hashNode = jsonNodeFactory.numberNode(sampler.getFileMdHash());
        rootNode.set("hash", hashNode);

        // Sampler
        ObjectNode samplerNode = jsonNodeFactory.objectNode();

        // Sampler - SamplerConfig
        NumericNode accuracyXNode = jsonNodeFactory.numberNode(sampler.getSamplerConfig_cached().getAccuracyX());
        NumericNode accuracyYNode = jsonNodeFactory.numberNode(sampler.getSamplerConfig_cached().getAccuracyY());
        NumericNode passesPerBlockNode = jsonNodeFactory.numberNode(sampler.getSamplerConfig_cached().getPassesPerBlock());
        samplerNode.set("accuracyX", accuracyXNode);
        samplerNode.set("accuracyY", accuracyYNode);
        samplerNode.set("passesPerBlock", passesPerBlockNode);

        // Sampler - Fingerprint
        ArrayNode fingerprintNode = mapper.valueToTree(sampler.getFingerprint(sampler.getSamplerConfig_cached()));
        samplerNode.set("fingerprint", fingerprintNode);

        rootNode.set("sampler", samplerNode);

        return rootNode;
    }

    private void loadCache() {
        File cacheFile = new File(absoluteCachePath);
        try {
            String fileString = FileHandlerUtil.readFileToString(cacheFile);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(fileString);

            cache = hashCacheAsPojo(json);

        } catch (IOException e) {
            System.out.println("Couldn't load cache: " + cacheFile.getAbsolutePath() + ", proceeding without a cache.");
        }
    }

    private Map<Integer, Sampler> hashCacheAsPojo(JsonNode root) {
        ObjectMapper mapper = new ObjectMapper();
        Map<Integer, Sampler> hashersWithSamplers = new HashMap<>();

        // For each Sampler-Hash key-value pair
        for (JsonNode node : root) {
            JsonNode samplerNode = node.get("sampler");
            ArrayNode fingerprintNode = (ArrayNode) samplerNode.get("fingerprint");
            NumericNode hashNode = (NumericNode) node.get("hash");

            NumericNode accuracyXNode = (NumericNode) samplerNode.get("accuracyX");
            NumericNode accuracyYNode = (NumericNode) samplerNode.get("accuracyY");
            NumericNode passesPerBlockNode = (NumericNode) samplerNode.get("passesPerBlock");

            // Constructing parametric type Pair<Point, SimpleColor>
            JavaType type = mapper.getTypeFactory().constructParametricType(SimplePair.class, Point.class, SimpleColor.class);

            ObjectReader reader = mapper.readerFor(type);
            List<SimplePair<Point, SimpleColor>> fingerprint = new ArrayList<>();
            try {
                // For each sample taken (key x,y : value r,g,b,a)
                for (JsonNode discreteSample : fingerprintNode) {
                    fingerprint.add(reader.readValue(discreteSample));
                }
                SamplerConfig samplerConfig = new SamplerConfig(
                        accuracyXNode.asInt(), accuracyYNode.asInt(), passesPerBlockNode.asInt());
                Sampler sampler = new Sampler(fingerprint, samplerConfig);

                hashersWithSamplers.put(hashNode.asInt(), sampler);

            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Couldn't parse cache. Maybe it is corrupt?");
            }
        }
        return hashersWithSamplers;
    }
}
