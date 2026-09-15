package com.homework.checker;

import com.homework.checker.exception.EmptyContentException;
import com.homework.checker.exception.FileReadException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 单元测试：覆盖正常流程、边界条件与异常处理，共 15 个用例。
 *
 * <p>构造思路（白盒为主）：
 * 1. 纯函数层（Tokenizer/VectorBuilder/SimilarityCalculator）直接构造输入验证输出；
 * 2. 流程层用 @TempDir 生成临时文件，模拟评测机的文件输入输出；
 * 3. 异常层验证自定义异常与退出码约定。</p>
 */
@DisplayName("论文查重单元测试")
class DuplicateCheckerTest {

    /** 题目中的样例原文与抄袭版。 */
    private static final String ORIGINAL = "今天是星期天，天气晴，今天晚上我要去看电影。";
    private static final String PLAGIARIZED = "今天是周天，天气晴朗，我晚上要去看电影。";

    @Nested
    @DisplayName("Tokenizer 分词")
    class TestTokenizer {

        @Test
        @DisplayName("正常中文文本应被切分且不含空白 token")
        void tokenizeFiltersWhitespace() {
            List<String> words = Tokenizer.tokenize("今天 天气\n晴");
            assertFalse(words.isEmpty());
            for (String word : words) {
                assertFalse(word.trim().isEmpty(), "分词结果不应包含空白 token");
            }
        }

        @Test
        @DisplayName("空文本应抛出 EmptyContentException")
        void tokenizeEmptyThrows() {
            assertThrows(EmptyContentException.class, () -> Tokenizer.tokenize(""));
            assertThrows(EmptyContentException.class, () -> Tokenizer.tokenize("   \n  "));
        }

        @Test
        @DisplayName("英文与数字应统一转小写")
        void tokenizeLowercase() {
            List<String> words = Tokenizer.tokenize("Hello World 2026");
            for (String word : words) {
                assertEquals(word.toLowerCase(), word);
            }
        }
    }

    @Nested
    @DisplayName("VectorBuilder 与 SimilarityCalculator")
    class TestVectorAndCosine {

        private Map<String, Integer> vectorOf(Object... pairs) {
            Map<String, Integer> vector = new HashMap<>();
            for (int i = 0; i < pairs.length; i += 2) {
                vector.put((String) pairs[i], (Integer) pairs[i + 1]);
            }
            return vector;
        }

        @Test
        @DisplayName("词频统计应正确累加出现次数")
        void buildVectorCounts() {
            Map<String, Integer> vector = VectorBuilder.build(Arrays.asList("今天", "天气", "今天"));
            assertEquals(2, vector.get("今天"));
            assertEquals(1, vector.get("天气"));
        }

        @Test
        @DisplayName("完全相同的向量相似度应为 1.0")
        void cosineIdentical() {
            Map<String, Integer> v = vectorOf("今天", 2, "天气", 1);
            assertEquals(1.0, SimilarityCalculator.cosine(v, new HashMap<>(v)), 1e-9);
        }

        @Test
        @DisplayName("没有任何公共词时相似度应为 0.0")
        void cosineDisjoint() {
            Map<String, Integer> a = vectorOf("今天", 1);
            Map<String, Integer> b = vectorOf("明天", 1);
            assertEquals(0.0, SimilarityCalculator.cosine(a, b), 1e-9);
        }

        @Test
        @DisplayName("部分重叠时相似度应落在 (0, 1) 区间")
        void cosinePartialOverlap() {
            Map<String, Integer> a = vectorOf("今天", 2, "天气", 1, "晴", 1);
            Map<String, Integer> b = vectorOf("今天", 2, "晴朗", 1, "晴", 1);
            double similarity = SimilarityCalculator.cosine(a, b);
            assertTrue(similarity > 0.0 && similarity < 1.0, "相似度=" + similarity);
        }
    }

    @Nested
    @DisplayName("TextReader 文件读取")
    class TestTextReader {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("应能正常读取 UTF-8 文件")
        void readUtf8() throws IOException {
            Path file = tempDir.resolve("utf8.txt");
            Files.write(file, ORIGINAL.getBytes(StandardCharsets.UTF_8));
            assertEquals(ORIGINAL, TextReader.read(file.toString()));
        }

        @Test
        @DisplayName("应能兼容读取 GBK 编码文件")
        void readGbk() throws IOException {
            Path file = tempDir.resolve("gbk.txt");
            Files.write(file, ORIGINAL.getBytes(Charset.forName("GBK")));
            assertEquals(ORIGINAL, TextReader.read(file.toString()));
        }

        @Test
        @DisplayName("读取不存在的文件应抛出 FileReadException")
        void readNotExists() {
            Path ghost = tempDir.resolve("ghost.txt");
            assertThrows(FileReadException.class, () -> TextReader.read(ghost.toString()));
        }
    }

    @Nested
    @DisplayName("DuplicateChecker 完整流程")
    class TestCheckerFlow {

        @TempDir
        Path tempDir;

        private Path write(String name, String content) throws IOException {
            Path file = tempDir.resolve(name);
            Files.write(file, content.getBytes(StandardCharsets.UTF_8));
            return file;
        }

        @Test
        @DisplayName("完全相同的文本重复率应为 1.00")
        void identicalText() throws IOException {
            Path a = write("a.txt", ORIGINAL);
            Path b = write("b.txt", ORIGINAL);
            assertEquals(1.0, DuplicateChecker.check(a.toString(), b.toString()), 1e-9);
        }

        @Test
        @DisplayName("题目样例（轻度增删改）重复率应大于 0.5")
        void samplePair() throws IOException {
            Path a = write("orig.txt", ORIGINAL);
            Path b = write("orig_add.txt", PLAGIARIZED);
            assertTrue(DuplicateChecker.check(a.toString(), b.toString()) > 0.5);
        }

        @Test
        @DisplayName("完全不同的文本重复率应低于 0.1")
        void totallyDifferent() throws IOException {
            Path a = write("a.txt", "苹果香蕉橘子葡萄西瓜芒果荔枝");
            Path b = write("b.txt", "数据库操作系统计算机网络组成原理");
            assertTrue(DuplicateChecker.check(a.toString(), b.toString()) < 0.1);
        }

        @Test
        @DisplayName("空文件应抛出 EmptyContentException")
        void emptyFileThrows() throws IOException {
            Path a = write("full.txt", ORIGINAL);
            Path empty = write("empty.txt", "");
            assertThrows(EmptyContentException.class,
                    () -> DuplicateChecker.check(a.toString(), empty.toString()));
        }
    }

    @Nested
    @DisplayName("Main 命令行入口")
    class TestFileIOAndMain {

        @TempDir
        Path tempDir;

        private Path write(String name, String content) throws IOException {
            Path file = tempDir.resolve(name);
            Files.write(file, content.getBytes(StandardCharsets.UTF_8));
            return file;
        }

        @Test
        @DisplayName("完整命令行流程：答案文件应写入两位小数")
        void mainSuccessAndAnswerFormat() throws IOException {
            Path orig = write("orig.txt", ORIGINAL);
            Path add = write("orig_add.txt", PLAGIARIZED);
            Path ans = tempDir.resolve("ans.txt");

            int exitCode = Main.run(new String[]{orig.toString(), add.toString(), ans.toString()});

            assertEquals(Main.EXIT_OK, exitCode);
            String content = new String(Files.readAllBytes(ans), StandardCharsets.UTF_8).trim();
            assertTrue(content.matches("\\d\\.\\d{2}"), "答案应为两位小数: " + content);
        }

        @Test
        @DisplayName("命令行参数个数错误应返回退出码 1")
        void mainWrongArgs() {
            assertEquals(Main.EXIT_BAD_ARGS, Main.run(new String[]{"only-one-arg"}));
            assertEquals(Main.EXIT_BAD_ARGS, Main.run(new String[]{}));
        }

        @Test
        @DisplayName("命令行传入不存在的文件应返回退出码 2")
        void mainMissingFile() {
            int exitCode = Main.run(new String[]{
                    tempDir.resolve("ghost.txt").toString(),
                    tempDir.resolve("ghost2.txt").toString(),
                    tempDir.resolve("ans.txt").toString()});
            assertEquals(Main.EXIT_RUNTIME_ERROR, exitCode);
        }

        @Test
        @DisplayName("命令行传入空文件应返回退出码 2")
        void mainEmptyFile() throws IOException {
            Path orig = write("orig.txt", ORIGINAL);
            Path empty = write("empty.txt", "");
            int exitCode = Main.run(new String[]{
                    orig.toString(), empty.toString(), tempDir.resolve("ans.txt").toString()});
            assertEquals(Main.EXIT_RUNTIME_ERROR, exitCode);
        }

        @Test
        @DisplayName("writeAnswer 应按 %.2f 格式化")
        void writeAnswerFormat() throws IOException {
            Path ans = tempDir.resolve("ans.txt");
            Main.writeAnswer(ans.toString(), 0.9456);
            assertEquals("0.95", new String(Files.readAllBytes(ans), StandardCharsets.UTF_8).trim());
        }
    }
}
