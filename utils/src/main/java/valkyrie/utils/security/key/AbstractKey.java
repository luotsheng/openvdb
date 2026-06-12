package valkyrie.utils.security.key;

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

import valkyrie.utils.TypeConverter;
import valkyrie.utils.security.Codec;
import valkyrie.utils.string.StrStaticImports;

import java.security.Key;

/**
 * @author Luo Tiansheng
 */
public abstract class AbstractKey {

    private final byte[] encoded;

    public AbstractKey(Key key) {
        this(key.getEncoded());
    }

    public AbstractKey(byte[] encoded) {
        this.encoded = encoded;
    }

    public byte[] getEncoded() {
        return encoded;
    }

    public abstract String toPEMFormat();

    public static byte[] decodePEMFormat(String pem) {
        String[] lines = StrStaticImports.strtok(pem, "\n");
        StringBuilder remakeBuilder = new StringBuilder();
        for (int i = 1; i < (lines.length - 1); i++) {
            remakeBuilder.append(StrStaticImports.strcut(lines[i], 0, 0));
        }
        return Codec.BASE64.decodeBytes(TypeConverter.atos(remakeBuilder));
    }

    protected static String toPEMFormat0(String keyType, byte[] encoded) {
        StringBuilder secretBuilder = new StringBuilder();

        String base64Encode = Codec.BASE64.encode(encoded);
        int encodeLength = StrStaticImports.strlen(base64Encode);
        int len = 64;
        int loopCount = encodeLength / len;
        int copyLength = 0;

        for (int i = 0; i < loopCount; i++) {
            int off = i * len;
            if (off + len > encodeLength)
                len = Math.abs((off + len) - encodeLength);
            secretBuilder.append(StrStaticImports.strcut(base64Encode, off, len)).append("\n");
            copyLength += len;
        }

        // 检查是否还有剩余内容
        if (copyLength < encodeLength)
            secretBuilder.append(StrStaticImports.strcut(base64Encode, copyLength, 0)).append("\n");

        secretBuilder.delete(secretBuilder.length() - 1, secretBuilder.length());
        return "-----BEGIN " + keyType + "-----\n" + TypeConverter.atos(secretBuilder) + "\n-----END " + keyType + "-----";
    }

    public String toZipKeyFormat() {
        return Codec.BASE64.encode(encoded);
    }

    @Override
    public String toString() {
        return toZipKeyFormat();
    }
}
