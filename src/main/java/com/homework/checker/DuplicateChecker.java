package com.homework.checker;

import java.util.List;
import java.util.Map;

/**
 * 查重组装层：串联 读取 -> 分词 -> 向量化 -> 相似度计算 的完整流程。
 */
public final class DuplicateChecker {

    private DuplicateChecker() {
        // 工具类，禁止实例化
    }

    /**
     * 计算原文与抄袭版的重复率。
     *
     * @param originalPath   原文文件路径
     * @param plagiarizedPath 抄袭版文件路径
     * @return 重复率，范围 [0.0, 1.0]
     */
    public static double check(String originalPath, String plagiarizedPath) {
        String originalText = TextReader.read(originalPath);
        String plagiarizedText = TextReader.read(plagiarizedPath);

        List<String> originalWords = Tokenizer.tokenize(originalText);
        List<String> plagiarizedWords = Tokenizer.tokenize(plagiarizedText);

        Map<String, Integer> originalVector = VectorBuilder.build(originalWords);
        Map<String, Integer> plagiarizedVector = VectorBuilder.build(plagiarizedWords);

        return SimilarityCalculator.cosine(originalVector, plagiarizedVector);
    }
}
