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
package io.agentscope.runtime.engine.services.memory.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Text embedding service interface
 */
public interface EmbeddingService {
    
    /**
     * Convert text to vector embedding
     *
     * @param text input text
     * @return vector embedding
     */
    CompletableFuture<List<Double>> embedText(String text);
    
    /**
     * Calculate cosine similarity between two vectors
     *
     * @param vector1 vector 1
     * @param vector2 vector 2
     * @return cosine similarity (between 0-1, 1 means completely identical)
     */
    double cosineSimilarity(List<Double> vector1, List<Double> vector2);
    
    /**
     * Calculate Euclidean distance between two vectors
     *
     * @param vector1 vector 1
     * @param vector2 vector 2
     * @return Euclidean distance
     */
    double euclideanDistance(List<Double> vector1, List<Double> vector2);
    
    /**
     * Get the dimension of embedding vectors
     *
     * @return vector dimension
     */
    int getEmbeddingDimension();
}
