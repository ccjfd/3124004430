package com.homework.checker;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.homework.checker.exception.EmptyContentException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 分词器：基于 jieba 中文分词，将原文切分为词列表。
 *
 * <p>设计要点：只过滤空白 token（空格、换行等），
 * 保留标点符号参与词频统计，使重复率的计算更贴近原文与
 * 抄袭版之间的真实差异（标点改动也会被计入）。</p>
 */
public final class Tokenizer {

    /** jieba 分词器实例。词典加载成本高，进程内共享一个实例。 */
    private static final JiebaSegmenter SEGMENTER = new JiebaSegmenter();

    private Tokenizer() {
        // 工具类，禁止实例化
    }

    /**
     * 对文本进行中文分词，返回有效词列表（不含空白）。
     *
     * @param text 待分词文本
     * @return 词列表
     * @throws EmptyContentException 文本为空或分词后无有效内容
     */
    public static List<String> tokenize(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new EmptyContentException("文本内容为空，无法计算相似度");
        }
        List<String> words = new ArrayList<>();
        // 使用 SEARCH 模式：在 INDEX 精确分词基础上再对长词切分，召回率更高
        for (com.huaban.analysis.jieba.SegToken token : SEGMENTER.process(text, JiebaSegmenter.SegMode.SEARCH)) {
            String word = token.word;
            if (!word.trim().isEmpty()) {
                words.add(word.toLowerCase(Locale.ROOT));
            }
        }
        if (words.isEmpty()) {
            throw new EmptyContentException("文本分词后无有效内容，无法计算相似度");
        }
        return words;
    }
}
