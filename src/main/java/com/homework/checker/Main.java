package com.homework.checker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 命令行入口。
 *
 * <p>用法：java -jar main.jar [原文文件] [抄袭版论文的文件] [答案文件]</p>
 *
 * <p>退出码约定（评测机据此判断程序是否异常退出）：
 * 0 = 成功；1 = 命令行参数个数错误；2 = 运行期异常（文件不存在/为空/IO错误）。</p>
 */
public final class Main {

    /** 程序退出码：成功 */
    static final int EXIT_OK = 0;
    /** 程序退出码：参数错误 */
    static final int EXIT_BAD_ARGS = 1;
    /** 程序退出码：运行期异常 */
    static final int EXIT_RUNTIME_ERROR = 2;

    private Main() {
        // 入口类，禁止实例化
    }

    public static void main(String[] args) {
        int exitCode = run(args);
        if (exitCode != EXIT_OK) {
            System.exit(exitCode);
        }
    }

    /**
     * 独立于 main 的运行逻辑，便于单元测试捕获退出码。
     *
     * @param args 命令行参数
     * @return 退出码
     */
    static int run(String[] args) {
        if (args == null || args.length != 3) {
            System.out.println("用法: java -jar main.jar [原文文件] [抄袭版论文的文件] [答案文件]");
            return EXIT_BAD_ARGS;
        }
        try {
            double similarity = DuplicateChecker.check(args[0], args[1]);
            writeAnswer(args[2], similarity);
            return EXIT_OK;
        } catch (RuntimeException e) {
            System.out.println("[错误] " + e.getMessage());
            return EXIT_RUNTIME_ERROR;
        }
    }

    /**
     * 把重复率写入答案文件，保留两位小数（题目要求输出浮点型，精确到小数点后两位）。
     *
     * @param answerPath 答案文件路径
     * @param similarity 重复率
     */
    static void writeAnswer(String answerPath, double similarity) {
        Path path = Paths.get(answerPath);
        String content = String.format("%.2f", similarity);
        try {
            Files.write(path, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new com.homework.checker.exception.FileReadException(
                    "写入答案文件失败: " + path.toAbsolutePath(), e);
        }
    }
}
