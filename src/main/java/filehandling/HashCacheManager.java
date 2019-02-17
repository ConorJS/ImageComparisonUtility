package filehandling;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import imaging.sampler.FingerprintConfig;
import imaging.sampler.Sampler;
import imaging.util.SimpleColor;
import imaging.util.SimplePair;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HashCacheManager extends CacheManager<Sampler, Integer> {

    // ========= ATTRIBUTES ===========================================================================================

    private static final String CACHE_DIRECTORY = ".duplicate_detection";
    private static final String CACHE_FILE = "cache";

    @Getter // override
    @Setter // override
    private Map<Integer, Sampler> cache;

    @Getter // override
    private Map<Integer, Sampler> newCache = new HashMap<>();

    // ========= CONSTRUCTOR ==========================================================================================
    public HashCacheManager(String imageFolderPath) {
        super(imageFolderPath);
    }

    // ========= ABSTRACT CLASS IMPLEMENTATION DETAILS ================================================================
    @Override
    void cache(Sampler sampler, Integer fileMdHash) {
        newCache.put(sampler.getFileMdHash(), sampler);
    }

    @Override
    String getCacheDirectory() {
        return CACHE_DIRECTORY;
    }

    @Override
    String getCacheFile() {
        return CACHE_FILE;
    }

    @Override
    JsonNode samplerAsJson(Sampler sampler) {
        ObjectMapper mapper = new ObjectMapper();

        // Root
        ObjectNode rootNode = jsonNodeFactory.objectNode();

        // Hash
        NumericNode hashNode = jsonNodeFactory.numberNode(sampler.getFileMdHash());
        rootNode.set("hash", hashNode);

        // Sampler
        ObjectNode samplerNode = jsonNodeFactory.objectNode();

        // Sampler - FingerprintConfig
        NumericNode accuracyXNode = jsonNodeFactory.numberNode(sampler.getFingerprintConfig_cached().getAccuracyX());
        NumericNode accuracyYNode = jsonNodeFactory.numberNode(sampler.getFingerprintConfig_cached().getAccuracyY());
        NumericNode passesPerBlockNode = jsonNodeFactory.numberNode(sampler.getFingerprintConfig_cached().getPassesPerBlock());
        NumericNode noiseScoreNode = jsonNodeFactory.numberNode(sampler.getNoiseScore().doubleValue());
        samplerNode.set("accuracyX", accuracyXNode);
        samplerNode.set("accuracyY", accuracyYNode);
        samplerNode.set("passesPerBlock", passesPerBlockNode);
        samplerNode.set("noiseScore", noiseScoreNode);

        // Sampler - Fingerprint
        ArrayNode fingerprintNode = mapper.valueToTree(sampler.getFingerprint(sampler.getFingerprintConfig_cached()));
        samplerNode.set("fingerprint", fingerprintNode);

        rootNode.set("sampler", samplerNode);

        return rootNode;
    }

    @Override
    Map<Integer, Sampler> hashCacheAsPOJO(JsonNode root) {
        ObjectMapper mapper = new ObjectMapper();
        Map<Integer, Sampler> hashesWithSamplers = new HashMap<>();

        // For each Sampler-Hash key-value pair
        for (JsonNode node : root) {
            JsonNode samplerNode = node.get("sampler");
            ArrayNode fingerprintNode = (ArrayNode) samplerNode.get("fingerprint");
            NumericNode hashNode = (NumericNode) node.get("hash");

            NumericNode accuracyXNode = (NumericNode) samplerNode.get("accuracyX");
            NumericNode accuracyYNode = (NumericNode) samplerNode.get("accuracyY");
            NumericNode passesPerBlockNode = (NumericNode) samplerNode.get("passesPerBlock");
            NumericNode noiseScoreNode = (NumericNode) samplerNode.get("noiseScore");

            // Constructing parametric type Pair<Point, SimpleColor>
            JavaType type = mapper.getTypeFactory().constructParametricType(SimplePair.class, Point.class, SimpleColor.class);

            ObjectReader reader = mapper.readerFor(type);
            List<SimplePair<Point, SimpleColor>> fingerprint = new ArrayList<>();
            try {
                // For each sample taken (key x,y : value r,g,b,a)
                for (JsonNode discreteSample : fingerprintNode) {
                    fingerprint.add(reader.readValue(discreteSample));
                }
                FingerprintConfig fingerprintConfig = new FingerprintConfig(
                        accuracyXNode.asInt(), accuracyYNode.asInt(), passesPerBlockNode.asInt());

                Sampler sampler = new Sampler(fingerprint, fingerprintConfig, noiseScoreNode.asDouble());

                hashesWithSamplers.put(hashNode.asInt(), sampler);

            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Couldn't parse cache. Maybe it is corrupt?");
            }
        }

        return hashesWithSamplers;
    }

    // ========= HASH-CACHE-MANAGER METHODS ===========================================================================

    // -- Simpler cache implementation
    public void cache(Sampler sampler) {
        cache(sampler, sampler.getFileMdHash());
    }

    // -- Use this instead of the abstract loadCachedObject(Integer)
    public Sampler loadCachedSampler(Integer imageFileHash) {
        // Returns a copy of a sampler, we don't want two files
        // sharing the same hash to point to the same Sampler object
        return loadCachedObject(imageFileHash).copy();
    }

    // -- Use this instead of the abstract isCached(Integer)
    // Calls abstract implementation first, then does another check to see if the cached
    // Sampler was cached with the same fingerprint configuration provided
    public boolean isCached(Integer imageFileHash, FingerprintConfig config) {
        return isCached(imageFileHash) && getCache().get(imageFileHash).getFingerprintConfig_cached().equals(config);
    }
}
