package com.homework.checker;

import java.util.Map;

/**
 * 相似度计算器：基于词频向量的余弦相似度。
 *
 * <p>算法关键：similarity = (A·B) / (|A| × |B|)。
 * 分子只遍历两篇文档的公共词（点积），分母为各自向量模长的乘积，
 * 时间复杂度 O(n1 + n2)，n1、n2 为两篇文档的词数。
 * 对增、删、改均鲁棒，且与语序无关。</p>
 */
public final class SimilarityCalculator {

    private SimilarityCalculator() {
        // 工具类，禁止实例化
    }

    /**
     * 计算两个词频向量的余弦相似度，结果范围 [0.0, 1.0]。
     *
     * @param vectorA 原文词频向量
     * @param vectorB 抄袭版词频向量
     * @return 余弦相似度
     */
    public static double cosine(Map<String, Integer> vectorA, Map<String, Integer> vectorB) {
        // 点积：只遍历较小向量，在较大向量中查找公共词
        Map<String, Integer> small = vectorA.size() <= vectorB.size() ? vectorA : vectorB;
        Map<String, Integer> large = small == vectorA ? vectorB : vectorA;

        double dotProduct = 0.0;
        for (Map.Entry<String, Integer> entry : small.entrySet()) {
            Integer countInLarge = large.get(entry.getKey());
            if (countInLarge != null) {
                dotProduct += (double) entry.getValue() * countInLarge;
            }
        }

        // 各自向量模长（欧氏范数）
        double normA = norm(vectorA);
        double normB = norm(vectorB);
        if (normA == 0.0 || normB == 0.0) {
            return 0.0; // 防御性判断：空向量不会出现，除零保护
        }
        double similarity = dotProduct / (normA * normB);
        // 浮点误差可能造成略大于 1，钳位到 [0,1]
        return Math.max(0.0, Math.min(1.0, similarity));
    }

    /** 计算向量模长 sqrt(sum(v^2))。 */
    private static double norm(Map<String, Integer> vector) {
        double sumOfSquares = 0.0;
        for (Integer count : vector.values()) {
            sumOfSquares += (double) count * count;
        }
        return Math.sqrt(sumOfSquares);
    }
}
