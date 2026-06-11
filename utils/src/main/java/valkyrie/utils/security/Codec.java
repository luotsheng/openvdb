package valkyrie.utils.security;

/* -------------------------------------------------------------------------------- *\
|*                                                                                  *|
|*    Copyright (C) 2019-2024 Luo Tiansheng All rights reserved.                    *|
|*                                                                                  *|
|*    Licensed under the Apache License, Version 2.0 (the "License");               *|
|*    you may not use this file except in compliance with the License.              *|
|*    You may obtain a copy of the License at                                       *|
|*                                                                                  *|
|*        http://www.apache.org/licenses/LICENSE-2.0                                *|
|*                                                                                  *|
|*    Unless required by applicable law or agreed to in writing, software           *|
|*    distributed under the License is distributed on an "AS IS" BASIS,             *|
|*    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.      *|
|*    See the License for the specific language governing permissions and           *|
|*    limitations under the License.                                                *|
|*                                                                                  *|
\* -------------------------------------------------------------------------------- */

/* Creates on 2025/2/20. */


import valkyrie.utils.Captor;
import valkyrie.utils.TypeConverter;
import valkyrie.utils.security.codec.Base64Codec;
import valkyrie.utils.security.codec.MD5Codec;
import valkyrie.utils.security.codec.SHA256Codec;
import valkyrie.utils.security.codec.URLCodec;
import valkyrie.utils.string.StrStaticImports;
import valkyrie.utils.string.StringInterface;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * `Codec` 类用于数据的编码和解码操作。它提供了将数据从一种格式转换为另一种格式的功能，
 * 通常用于压缩、加密或其他数据转换需求。
 *
 * <p>本类的实现可能支持多种编码和解码格式，允许用户在不同的数据表示之间进行转换。
 *
 * <h2>注意事项</h2>
 * <ul>
 *     <li>本类的实现需要保证编码与解码的一致性，以确保数据能够被正确还原。</li>
 *     <li>根据使用的编码/解码算法，可能存在性能差异或安全性考虑。</li>
 * </ul>
 *
 * @see Base64Codec
 * @see MD5Codec
 * @see SHA256Codec
 * @see URLCodec
 * @author Luo Tiansheng
 */
public class Codec {

    public static final Base64 BASE64 = new Base64Codec(); // BASE64
    public static final MD5    MD5    = new MD5Codec();    // MD5
    public static final SHA256 SHA256 = new SHA256Codec(); // SHA-256
    public static final URL    URL    = new URLCodec();    // URL

    /**
     * 生成一个版本号
     *
     * @param major
     *        主版本号
     *
     * @param minor
     *        副版本号
     *
     * @param patch
     *        补丁版本
     */
    public static int makeVersion(int major, int minor, int patch) {
        return major << 22 | minor << 12 | patch;
    }

    /**
     * 解析 {@link #makeVersion(int, int, int)} 生成的版本号，获取其中
     * 的主版本号内容并返回。
     *
     * @param version
     *        版本号
     *
     * @return 主版本号
     */
    public static int versionMajor(int version) {
        return version >> 22;
    }

    /**
     * 解析 {@link #makeVersion(int, int, int)} 生成的版本号，获取其中
     * 的次版本号内容并返回。
     *
     * @param version
     *        版本号
     *
     * @return 次版本号
     */
    public static int versionMinor(int version) {
        return version >> 12 & 0x3ff;
    }

    /**
     * 解析 {@link #makeVersion(int, int, int)} 生成的版本号，获取其中
     * 的补丁版本内容并返回。
     *
     * @param version
     *        版本号
     *
     * @return 补丁版本
     */
    public static int versionPatch(int version) {
        return version & 0xfff;
    }

    /**
     * 字节码转 16 进制
     */
    public static String toByteHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            String tmp = Integer.toHexString(b & 0xFF);
            if (StrStaticImports.strlen(tmp) == 1)
                builder.append("0");
            builder.append(tmp);
        }
        return TypeConverter.atos(builder, StringInterface.STRING_IFACE_UPPER_CASE_EXT);
    }

    /**
     * #brief: 生成并返回下一个随机的 AES 密钥，使用 Base64 编码格式表示
     *
     * <p>该方法利用 `KeyGenerator` 类生成一个 128 位的 AES 密钥，并将其编码为 Base64
     * 字符串以便于存储或传输。返回的密钥可用于加密或解密数据，确保数据的安全性。
     *
     * @return 生成的随机 AES 密钥的 Base64 编码字符串
     */
    public static String randomNextSecret() {
        return Captor.call(() -> {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128);
            SecretKey secretKey = keyGen.generateKey();
            return BASE64.encode(secretKey.getEncoded());
        });
    }

}
