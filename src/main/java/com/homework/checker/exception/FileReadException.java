package com.homework.checker.exception;

/**
 * 文件读取异常：文件不存在、不可读或 IO 失败时抛出。
 * 设计目标：把底层的 IOException 转换为业务可读的异常，
 * 使上层能统一打印 "[错误]" 信息并以约定的退出码 2 结束程序。
 */
public class FileReadException extends RuntimeException {
    public FileReadException(String message) {
        super(message);
    }

    public FileReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
