package filehandling;


import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

// T = object that is being cached
// I = unique identifier for object
public abstract class CacheManager<T, I> {

    final JsonNodeFactory jsonNodeFactory = new JsonNodeFactory(false);

    private final String cacheDirectoryAbsolutePath;
    private final String absolutePathToCache;

    // Need to implement getter for cache directory and file
    abstract String getCacheDirectory();
    abstract String getCacheFile();

    abstract Map<I, T> getCache();
    abstract void setCache(Map<I, T> cache);

    abstract Map<I, T> getNewCache();

    private String getCachePath() {
        return this.getCacheDirectory() + "\\" + this.getCacheFile();
    }

    CacheManager(String rootFolderPath) {
        cacheDirectoryAbsolutePath = rootFolderPath + "/" + getCacheDirectory();
        absolutePathToCache = rootFolderPath + "/" + getCachePath();
        loadCache();

        ObjectMapper mapper = new ObjectMapper();

        File cacheFile = new File(absolutePathToCache);
        if (cacheFile.exists()) {
            try {
                mapper.readTree(cacheFile);

            } catch (IOException e) {
                throw new RuntimeException("Couldn't read cache: " + absolutePathToCache + ". Maybe it is corrupt?");
            }
        } else {
            System.out.println("No cache detected, creating " + absolutePathToCache);
        }
    }

    private void loadCache() {
        File cacheFile = new File(absolutePathToCache);
        try {
            String fileString = FileHandlerUtil.readFileToString(cacheFile);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(fileString);

            setCache(hashCacheAsPOJO(json));

        } catch (IOException e) {
            System.out.println("Couldn't load cache: " + cacheFile.getAbsolutePath() + ", proceeding without a cache.");
        }
    }

    public void saveCache() {
        File cacheFile = new File(absolutePathToCache);
        try {
            File directory = new File(cacheDirectoryAbsolutePath);
            if (!directory.exists() && !directory.mkdir()) {
                throw new RuntimeException("Couldn't create cache directory.");
            }
            ObjectMapper mapper = new ObjectMapper();
            ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
            writer.writeValue(cacheFile, hashCacheAsJson(getNewCache().values()));
            System.out.println("Saved cache at: " + cacheFile.getAbsolutePath());

        } catch (IOException e) {
            throw new RuntimeException("Couldn't write cache: " + absolutePathToCache);
        }
    }

    T loadCachedObject(I identifier) {
        return getCache().get(identifier);
    }

    abstract void cache(T objectToCache, I identifier);

    // Cache hit if cache exists, a file with the same hash was fingerprinted + cached,
    // and that the cached fingerprint was performed with the same configuration as what is being requested
    boolean isCached(I identifier) {
        return (getCache() != null) &&
                getCache().containsKey(identifier);
    }

    abstract JsonNode samplerAsJson(T t);

    abstract Map<I, T> hashCacheAsPOJO(JsonNode root);

    private JsonNode hashCacheAsJson(Collection<T> samplers) {
        ArrayNode cache = jsonNodeFactory.arrayNode();
        for (T t : samplers) {
            cache.add(samplerAsJson(t));
        }

        return cache;
    }
}
