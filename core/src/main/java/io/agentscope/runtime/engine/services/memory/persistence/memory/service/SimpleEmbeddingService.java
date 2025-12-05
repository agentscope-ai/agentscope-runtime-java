/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.runtime.engine.services.memory.persistence.memory.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.agentscope.runtime.engine.services.memory.service.EmbeddingService;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

/**
 * Simple text embedding service implementation
 * Uses TF-IDF and term frequency statistics as vector representation
 */
public class SimpleEmbeddingService implements EmbeddingService {

    Logger logger = Logger.getLogger(SimpleEmbeddingService.class.getName());
    private static final int EMBEDDING_DIMENSION = 300; // Fixed dimension

    private final Map<String, Integer> vocabulary = new HashMap<>();
    private final Map<String, Integer> documentFrequencies = new HashMap<>();
    private int totalDocuments = 0;

    @Override
    public CompletableFuture<List<Double>> embedText(String text) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (text == null || text.trim().isEmpty()) {
                    return createZeroVector();
                }

                // Preprocess text
                String processedText = preprocessText(text);
                List<String> tokens = tokenize(processedText);

                if (tokens.isEmpty()) {
                    return createZeroVector();
                }

                // Compute TF-IDF vector
                List<Double> embedding = computeTfIdfVector(tokens);

                // Normalize vector
                normalizeVector(embedding);

                return embedding;

            } catch (Exception e) {
                logger.severe("Failed to generate text embedding" + e.getMessage());
                return createZeroVector();
            }
        });
    }

    @Override
    public double cosineSimilarity(List<Double> vector1, List<Double> vector2) {
        if (vector1 == null || vector2 == null || vector1.size() != vector2.size()) {
            return 0.0;
        }

        try {
            RealVector v1 = new ArrayRealVector(vector1.stream().mapToDouble(Double::doubleValue).toArray());
            RealVector v2 = new ArrayRealVector(vector2.stream().mapToDouble(Double::doubleValue).toArray());

            double dotProduct = v1.dotProduct(v2);
            double norm1 = v1.getNorm();
            double norm2 = v2.getNorm();

            if (norm1 == 0.0 || norm2 == 0.0) {
                return 0.0;
            }

            return dotProduct / (norm1 * norm2);

        } catch (Exception e) {
            logger.severe("Failed to compute cosine similarity" + e.getMessage());
            return 0.0;
        }
    }

    @Override
    public double euclideanDistance(List<Double> vector1, List<Double> vector2) {
        if (vector1 == null || vector2 == null || vector1.size() != vector2.size()) {
            return Double.MAX_VALUE;
        }

        try {
            RealVector v1 = new ArrayRealVector(vector1.stream().mapToDouble(Double::doubleValue).toArray());
            RealVector v2 = new ArrayRealVector(vector2.stream().mapToDouble(Double::doubleValue).toArray());

            return v1.getDistance(v2);

        } catch (Exception e) {
            logger.severe("Failed to compute Euclidean distance" + e.getMessage());
            return Double.MAX_VALUE;
        }
    }

    @Override
    public int getEmbeddingDimension() {
        return EMBEDDING_DIMENSION;
    }

    /**
     * Preprocess text
     */
    private String preprocessText(String text) {
        if (text == null) {
            return "";
        }

        return text.toLowerCase()
                .replaceAll("[^\\p{L}\\p{N}\\s]", " ") // Keep letters, numbers and spaces
                .replaceAll("\\s+", " ") // Merge multiple spaces
                .trim();
    }

    /**
     * Tokenize text
     */
    private List<String> tokenize(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(text.split("\\s+"))
                .filter(token -> token.length() > 1) // Filter single characters
                .collect(Collectors.toList());
    }

    /**
     * Compute TF-IDF vector
     */
    private List<Double> computeTfIdfVector(List<String> tokens) {
        // Compute term frequencies
        Map<String, Integer> termFrequencies = new HashMap<>();
        for (String token : tokens) {
            termFrequencies.put(token, termFrequencies.getOrDefault(token, 0) + 1);
        }

        // Update vocabulary and document frequencies
        updateVocabulary(termFrequencies.keySet());

        // Create vector
        List<Double> vector = new ArrayList<>(Collections.nCopies(EMBEDDING_DIMENSION, 0.0));

        for (Map.Entry<String, Integer> entry : termFrequencies.entrySet()) {
            String term = entry.getKey();
            int tf = entry.getValue();

            if (vocabulary.containsKey(term)) {
                int termIndex = vocabulary.get(term) % EMBEDDING_DIMENSION;
                double idf = computeIdf(term);
                double tfIdf = tf * idf;
                vector.set(termIndex, vector.get(termIndex) + tfIdf);
            }
        }

        return vector;
    }

    /**
     * Update vocabulary
     */
    private void updateVocabulary(Set<String> terms) {
        for (String term : terms) {
            vocabulary.putIfAbsent(term, vocabulary.size());
            documentFrequencies.put(term, documentFrequencies.getOrDefault(term, 0) + 1);
        }
        totalDocuments++;
    }

    /**
     * Compute IDF value
     */
    private double computeIdf(String term) {
        int df = documentFrequencies.getOrDefault(term, 1);
        return Math.log((double) totalDocuments / df);
    }

    /**
     * Normalize vector
     */
    private void normalizeVector(List<Double> vector) {
        double norm = Math.sqrt(vector.stream().mapToDouble(v -> v * v).sum());
        if (norm > 0) {
            for (int i = 0; i < vector.size(); i++) {
                vector.set(i, vector.get(i) / norm);
            }
        }
    }

    /**
     * Create zero vector
     */
    private List<Double> createZeroVector() {
        return new ArrayList<>(Collections.nCopies(EMBEDDING_DIMENSION, 0.0));
    }

    /**
     * Get vocabulary size
     */
    public int getVocabularySize() {
        return vocabulary.size();
    }

    /**
     * Get total document count
     */
    public int getTotalDocuments() {
        return totalDocuments;
    }
}
