package valkyrie.utils.security.codec;

import valkyrie.utils.exception.SystemRuntimeException;
import valkyrie.utils.security.MD5;
import valkyrie.utils.string.StrStaticImports;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Luo Tiansheng
 */
public class MD5Codec implements MD5 {

    /**
     * 十六进制字符集
     */
    static final char[] hexDigits = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f'
    };

    @Override
    public String lower32(String source) {
        return lower32(source.getBytes());
    }

    @Override
    public String lower32(byte[] b) {
        return md5(b, 32);
    }

    @Override
    public String lower16(String source) {
        return lower16(source.getBytes());
    }

    @Override
    public String lower16(byte[] b) {
        return md5(b, 16);
    }

    @Override
    public String upper32(String source) {
        return upper32(source.getBytes());
    }

    @Override
    public String upper32(byte[] b) {
        return lower32(b).toUpperCase();
    }

    @Override
    public String upper16(String source) {
        return upper16(source.getBytes());
    }

    @Override
    public String upper16(byte[] b) {
        return lower16(b).toUpperCase();
    }

    /** 使用 Java 原生函数生成 MD5 字符串。注意如果是获取 16 位的加密字符串，原始 MD5
     *  值会向前移动 8 位，然后截取出后 16 位返回。 */
    private String md5(byte[] b, int n) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(b);
            byte[] digests = md.digest();
            char[] chars = new char[32];
            int k = 0;
            for (int i = 0; i < 16; i++) {
                byte byte0 = digests[i];
                chars[k++] = hexDigits[byte0 >>> 4 & 0xf];
                chars[k++] = hexDigits[byte0 & 0xf];
            }
            /* 判断是获取 16 位的 md5 字符串还是 16 位的 */
            int flag = n > 16 ? 0 : 8;
            return StrStaticImports.strcut(chars, flag, (n + flag));
        } catch (NoSuchAlgorithmException e) {
            throw new SystemRuntimeException(e);
        }
    }

}
