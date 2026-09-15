package com.homework.checker;

import com.homework.checker.exception.FileReadException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文本读取器。
 *
 * <p>设计要点：评测用文本可能是 UTF-8，也可能是 GBK 编码。
 * 这里先按 UTF-8 严格解码（遇到非法字节立即报错而不是静默替换），
 * 失败则回退到 GBK，保证各种来源的 txt 文件都能正确读取。
 * 同时自动剔除 UTF-8 BOM 头。</p>
 */
public final class TextReader {

    private TextReader() {
        // 工具类，禁止实例化
    }

    /**
     * 读取整个文本文件（自动兼容 UTF-8 / GBK 编码）。
     *
     * @param filePath 文件的绝对或相对路径
     * @return 文件全部文本内容
     * @throws FileReadException 文件不存在或读取失败
     */
    public static String read(String filePath) {
        Path path = Paths.get(filePath);
        if (!Files.isReadable(path)) {
            throw new FileReadException("无法读取文件: " + path.toAbsolutePath());
        }
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new FileReadException("读取文件失败: " + path.toAbsolutePath(), e);
        }
        // 剔除 UTF-8 BOM（EF BB BF）
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            byte[] withoutBom = new byte[bytes.length - 3];
            System.arraycopy(bytes, 3, withoutBom, 0, withoutBom.length);
            bytes = withoutBom;
        }
        return decode(bytes);
    }

    /** 先按 UTF-8 严格解码，失败则回退 GBK。 */
    private static String decode(byte[] bytes) {
        try {
            return strictDecode(bytes, StandardCharsets.UTF_8);
        } catch (CharacterCodingException e) {
            try {
                return strictDecode(bytes, Charset.forName("GBK"));
            } catch (CharacterCodingException e2) {
                // 两种编码都解不了，退化为宽松的 UTF-8（替换损坏字符）
                return new String(bytes, StandardCharsets.UTF_8);
            }
        }
    }

    /** 严格模式解码：非法字节直接抛异常，便于判断编码是否匹配。 */
    private static String strictDecode(byte[] bytes, Charset charset) throws CharacterCodingException {
        CharsetDecoder decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        return decoder.decode(ByteBuffer.wrap(bytes)).toString();
    }
}
