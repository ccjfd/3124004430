package com.homework.checker.exception;

/**
 * 空内容异常：任一输入文件为空（或分词后无有效内容）时抛出。
 * 设计目标：空文本无法计算相似度，提前拦截并给出明确提示，
 * 避免输出误导性的 "0.00" 或产生除零错误。
 */
public class EmptyContentException extends RuntimeException {
    public EmptyContentException(String message) {
        super(message);
    }
}
