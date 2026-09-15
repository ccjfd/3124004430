package com.homework.checker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 向量构建器：把词列表转换为词频向量（词 -> 出现次数）。
 */
public final class VectorBuilder {

    private VectorBuilder() {
        // 工具类，禁止实例化
    }

    /**
     * 统计词频，构建词频向量。
     *
     * @param words 分词后的词列表
     * @return 词 -> 出现次数 的映射
     */
    public static Map<String, Integer> build(List<String> words) {
        Map<String, Integer> vector = new HashMap<>();
        for (String word : words) {
            // merge 一行完成"存在则累加、不存在则置 1"
            vector.merge(word, 1, Integer::sum);
        }
        return vector;
    }
}
