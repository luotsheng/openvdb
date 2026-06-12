package valkyrie.utils.string;

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


import valkyrie.utils.TypeConverter;

/**
 * 定义字符串操作类型的枚举类。
 *
 * <p>该枚举类用于列举常见的字符串操作类型。<p>
 *
 * <h2>功能特点</h2>
 * <ul>
 *     <li>易于扩展，支持后续增加更多字符串操作类型。</li>
 *     <li>提供统一的枚举类型，便于管理和使用不同的字符串操作。</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>
 *     strcut(a, off, len, STRING_IFACE_TRIM_EXT)
 * </pre>
 *
 * @author Luo Tiansheng
 * @since 1.0
 */
public enum StringInterface {

    /**
     * 字符串修剪操作，去除字符串前后的空白字符
     */
    STRING_IFACE_TRIM_EXT {
        @Override
        public String apply(String input) {
            return StrStaticImports.strip(input);
        }
    },

    /**
     * 将字符串转换为大写字母
     */
    STRING_IFACE_UPPER_CASE_EXT {
        @Override
        public String apply(String input) {
            return StrStaticImports.uppercase(input);
        }
    },

    /**
     * 将字符串转换为小写字母
     */
    STRING_IFACE_LOWER_CASE_EXT {
        @Override
        public String apply(String input) {
            return StrStaticImports.lowercase(input);
        }
    },

    /**
     * 将字符串首字母大写
     */
    STRING_IFACE_CAPITALIZE_EXT {
        @Override
        public String apply(String input) {
            return StrStaticImports.strcut(input, 0, 1, STRING_IFACE_UPPER_CASE_EXT) + StrStaticImports.strcut(input,1, 0);
        }
    },

    /**
     * 字符串反转
     */
    STRING_IFACE_REVERSE_EXT {
        @Override
        public String apply(String input) {
            return TypeConverter.atos(new StringBuilder(input).reverse());
        }
    },

    /**
     * 去除字符串中的所有空格
     */
    STRING_IFACE_REMOVE_SPACES_EXT {
        @Override
        public String apply(String input) {
            return StrStaticImports.strrexp(input, "\\s+", "");
        }
    },

    /**
     * 下划线格式转驼峰
     */
    STRING_IFACE_LINE_HMP_EXT {
        @Override
        public String apply(String input) {
            return StrStaticImports.strlinehmp(input);
        }
    }
    ;

    /** 应用字符串操作 */
    public abstract String apply(String input);

    /**
     * #brief：根据指定的字符串操作类型数组，对源字符串执行一系列操作。
     *
     * <p>该方法根据传入的操作数组，依次对源字符串进行处理。目前仅支持对字符串进行修剪操作，
     * 其他操作可以在未来扩展。若操作数组为 null，则直接返回源字符串。<p>
     *
     * <h2>功能特点</h2>
     * <ul>
     *     <li>支持对字符串进行按需操作（例如修剪空白字符）。</li>
     *     <li>通过枚举类型 {@link StringInterface} 扩展支持更多操作。</li>
     * </ul>
     *
     * <h2>使用示例</h2>
     * <pre>
     *     String result = pipelineExecutor("  Hello World  ", new StringOperator[]{StringOperator.STRING_OPERATOR_TRIM});
     *     // 返回 "Hello World"
     * </pre>
     *
     * @param source   源字符串
     * @param iface 要应用的字符串操作数组
     * @return 操作后的字符串
     */
    public static String pipelineExecutor(String source, StringInterface[] iface) {
        if (iface == null)
            return source;

        for (StringInterface iface_ext : iface)
            source = iface_ext.apply(source);

        return source;
    }

}