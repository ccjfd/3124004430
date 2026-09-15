package com.homework.checker;

import java.util.List;
import java.util.Map;

/**
 * <p>用法：java -cp main.jar com.homework.checker.Benchmark [原文] [抄袭版] [迭代次数]</p>
 *
 * <p>输出：各阶段（读取/分词/向量化/余弦计算）的平均耗时与总耗时，
 * 便于定位性能瓶颈。配合 JFR 使用可生成火焰图：</p>
 *
 * <pre>
 * java -XX:StartFlightRecording=filename=profile.jfr,settings=profile,dumponexit=true \
 *      -cp main.jar com.homework.checker.Benchmark testdata/orig.txt testdata/orig_0.8_dis_15.txt 30
 * </pre>
 */
public final class Benchmark {

    /** 预热轮数：让 JVM JIT 完成热点编译，避免把编译开销算进结果。 */
    private static final int WARMUP_ROUNDS = 3;

    private Benchmark() {
        // 工具类，禁止实例化
    }

    public static void main(String[] args) throws Exception {
        String originalPath = args.length > 0 ? args[0] : "testdata/orig.txt";
        String plagiarizedPath = args.length > 1 ? args[1] : "testdata/orig_0.8_add.txt";
        int rounds = args.length > 2 ? Integer.parseInt(args[2]) : 10;

        // 预热
        for (int i = 0; i < WARMUP_ROUNDS; i++) {
            DuplicateChecker.check(originalPath, plagiarizedPath);
        }

        long readNano = 0, tokenizeNano = 0, vectorNano = 0, cosineNano = 0;
        double lastSimilarity = 0.0;

        for (int i = 0; i < rounds; i++) {
            long t0 = System.nanoTime();
            String originalText = TextReader.read(originalPath);
            String plagiarizedText = TextReader.read(plagiarizedPath);
            long t1 = System.nanoTime();

            List<String> originalWords = Tokenizer.tokenize(originalText);
            List<String> plagiarizedWords = Tokenizer.tokenize(plagiarizedText);
            long t2 = System.nanoTime();

            Map<String, Integer> originalVector = VectorBuilder.build(originalWords);
            Map<String, Integer> plagiarizedVector = VectorBuilder.build(plagiarizedWords);
            long t3 = System.nanoTime();

            lastSimilarity = SimilarityCalculator.cosine(originalVector, plagiarizedVector);
            long t4 = System.nanoTime();

            readNano += t1 - t0;
            tokenizeNano += t2 - t1;
            vectorNano += t3 - t2;
            cosineNano += t4 - t3;
        }

        double readMs = readNano / 1e6 / rounds;
        double tokenizeMs = tokenizeNano / 1e6 / rounds;
        double vectorMs = vectorNano / 1e6 / rounds;
        double cosineMs = cosineNano / 1e6 / rounds;
        double totalMs = readMs + tokenizeMs + vectorMs + cosineMs;

        System.out.println("========== 性能基准（平均每轮，共 " + rounds + " 轮）==========");
        System.out.printf("读取文件        : %8.3f ms  (%5.1f%%)%n", readMs, readMs / totalMs * 100);
        System.out.printf("分词(jieba)     : %8.3f ms  (%5.1f%%)%n", tokenizeMs, tokenizeMs / totalMs * 100);
        System.out.printf("构建词频向量    : %8.3f ms  (%5.1f%%)%n", vectorMs, vectorMs / totalMs * 100);
        System.out.printf("余弦相似度计算  : %8.3f ms  (%5.1f%%)%n", cosineMs, cosineMs / totalMs * 100);
        System.out.printf("合计            : %8.3f ms%n", totalMs);
        System.out.printf("相似度结果      : %.4f%n", lastSimilarity);
        System.out.println("=========================================================");
    }
}
