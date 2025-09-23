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
package io.agentscope.runtime.engine.memory.persistence.memory.service;

import io.agentscope.runtime.engine.memory.service.EmbeddingService;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 简单的文本嵌入服务实现
 * 使用TF-IDF和词频统计作为向量表示
 */
public class SimpleEmbeddingService implements EmbeddingService {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleEmbeddingService.class);
    private static final int EMBEDDING_DIMENSION = 300; // 固定维度
    
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
                
                // 预处理文本
                String processedText = preprocessText(text);
                List<String> tokens = tokenize(processedText);
                
                if (tokens.isEmpty()) {
                    return createZeroVector();
                }
                
                // 计算TF-IDF向量
                List<Double> embedding = computeTfIdfVector(tokens);
                
                // 归一化向量
                normalizeVector(embedding);
                
                logger.debug("生成文本嵌入，文本长度: {}, 向量维度: {}", text.length(), embedding.size());
                
                return embedding;
                
            } catch (Exception e) {
                logger.error("生成文本嵌入失败", e);
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
            logger.error("计算余弦相似度失败", e);
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
            logger.error("计算欧几里得距离失败", e);
            return Double.MAX_VALUE;
        }
    }
    
    @Override
    public int getEmbeddingDimension() {
        return EMBEDDING_DIMENSION;
    }
    
    /**
     * 预处理文本
     */
    private String preprocessText(String text) {
        if (text == null) {
            return "";
        }
        
        return text.toLowerCase()
                .replaceAll("[^\\p{L}\\p{N}\\s]", " ") // 保留字母、数字和空格
                .replaceAll("\\s+", " ") // 合并多个空格
                .trim();
    }
    
    /**
     * 分词
     */
    private List<String> tokenize(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        return Arrays.stream(text.split("\\s+"))
                .filter(token -> token.length() > 1) // 过滤单字符
                .collect(Collectors.toList());
    }
    
    /**
     * 计算TF-IDF向量
     */
    private List<Double> computeTfIdfVector(List<String> tokens) {
        // 计算词频
        Map<String, Integer> termFrequencies = new HashMap<>();
        for (String token : tokens) {
            termFrequencies.put(token, termFrequencies.getOrDefault(token, 0) + 1);
        }
        
        // 更新词汇表和文档频率
        updateVocabulary(termFrequencies.keySet());
        
        // 创建向量
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
     * 更新词汇表
     */
    private void updateVocabulary(Set<String> terms) {
        for (String term : terms) {
            vocabulary.putIfAbsent(term, vocabulary.size());
            documentFrequencies.put(term, documentFrequencies.getOrDefault(term, 0) + 1);
        }
        totalDocuments++;
    }
    
    /**
     * 计算IDF值
     */
    private double computeIdf(String term) {
        int df = documentFrequencies.getOrDefault(term, 1);
        return Math.log((double) totalDocuments / df);
    }
    
    /**
     * 归一化向量
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
     * 创建零向量
     */
    private List<Double> createZeroVector() {
        return new ArrayList<>(Collections.nCopies(EMBEDDING_DIMENSION, 0.0));
    }
    
    /**
     * 获取词汇表大小
     */
    public int getVocabularySize() {
        return vocabulary.size();
    }
    
    /**
     * 获取总文档数
     */
    public int getTotalDocuments() {
        return totalDocuments;
    }
}
