package valkyrie.utils.time;

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

/* Creates on 2022/3/30. */

import valkyrie.utils.Captor;
import valkyrie.utils.exception.ValidationException;

import java.text.SimpleDateFormat;
import java.util.Date;

import static valkyrie.utils.string.StrStaticImports.strhas;
import static valkyrie.utils.string.StrStaticImports.strlen;

/**
 * `DateFormatter` 类提供日期和时间的格式化和解析功能。
 *
 * <p>该类支持将日期对象格式化为指定的字符串格式，以及将符合格式的字符串解析为日期对象。
 * 支持默认格式和自定义格式。<p>
 *
 * <h2>主要功能</h2>
 * <ul>
 *     <li>格式化当前时间或指定时间为字符串。</li>
 *     <li>将字符串解析为日期对象。</li>
 *     <li>支持默认日期格式和自定义日期格式。</li>
 * </ul>
 *
 * @author Luo Tiansheng
 * @since 1.0
 */
public class DateFormatter {

    private static final int STRING_TEMP_DATE_MONTH = 7; // strlen("0000-00");
    private static final int STRING_TEMP_DATE_DAY = 10; //strlen("0000-00-00");
    private static final int STRING_TEMP_DATE_TIME = 19; //strlen("0000-00-00 00:00:00");

    /** 短横线格式化风格 */
    public static final String DASH_PATTERN_Y4H2M2D2H2M2S2 = "yyyy-MM-dd HH:mm:ss";

    /** 短横线格式化风格 */
    public static final String SLASH_PATTERN_Y4H2M2D2H2M2S2 = "yyyy/MM/dd HH:mm:ss";

    /** 编译一个 Formatter 对象实例，根据 Pattern 编译。默认使用
     *  UnsafeFormatter 内置格式化对象。 */
    private static Formatter compileFormatter(String pattern) {
        return new LocalSimpleDateFormatter(pattern);
    }

    /** 日期格式化接口，为了方便适配多种日期格式化工具包抽象的
     *  接口层 */
    interface Formatter {
        /** 将日期格式化成字符串 */
        String format(Date date);
        /** 将字符串解析成日期 */
        Date parse(String text);
    }

    /**
     * 使用 SimpleDateFormatter 实现的 Formatter 接口。
     */
    static class LocalSimpleDateFormatter implements Formatter {
        private final SimpleDateFormat sdf;

        LocalSimpleDateFormatter(String pattern) {
            this.sdf = new SimpleDateFormat(pattern);
        }

        @Override
        public String format(Date date) {
            return sdf.format(date);
        }

        @Override
        public Date parse(String text) {
            return Captor.call(() -> sdf.parse(text));
        }
    }

    /** 返回当前时间 */
    public static Date now() {
        return new Date();
    }

    /** 当前时间戳 */
    public static long currentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * #brief：将当前时间格式化成 {@link #DASH_PATTERN_Y4H2M2D2H2M2S2} 风格<p>
     *
     * 将当前时间格式化成 {@link #DASH_PATTERN_Y4H2M2D2H2M2S2} 风格，也就是常用
     * 的：yyyy-MM-dd HH:mm:ss。该函数格式化当前日期。
     *
     * @return 格式化后的字符串
     */
    public static String format() {
        return format(now());
    }

    /**
     * #brief：将指定日期对象格式化为 {@link #DASH_PATTERN_Y4H2M2D2H2M2S2} 风格<p>
     *
     * 将指定日期对象格式化为 {@link #DASH_PATTERN_Y4H2M2D2H2M2S2} 风格，传入一个
     * 日期对象实例，然后将该日期对象格式化为 yyyy-MM-dd HH:mm:ss 字符串。
     *
     * @param date
     *        日期对象实例
     *
     * @return 格式化后的字符串
     */
    public static String format(Date date) {
        return format(date, DASH_PATTERN_Y4H2M2D2H2M2S2);
    }

    /**
     * #brief：将当前日期格式化为指定风格<p>
     *
     * 将当前日期格式化为指定风格，传入日期格式化表达式，类似：yyyy-MM-dd 格式化出来
     * 就是：2018-06-08 这样的日期风格。
     *
     * @param pattern
     *        自定义格式化规则
     *
     * @return 格式化后的字符串
     */
    public static String format(String pattern) {
        return format(now(), pattern);
    }

    /**
     * #brief：根据格式化规则将日期对象格式化为字符串<p>
     *
     * 根据格式化规则将日期对象格式化为字符串，通过 {@link SimpleDateFormat} 对象
     * 实现日期格式化。使用 `new` 关键字创建 {@link SimpleDateFormat} 对象实例满
     * 足线程安全要求。
     *
     * @param date
     *        格式化日期对象，如：“DateFormatter.now()”
     *
     * @param pattern
     *        格式化规则，如：“yyyy-MM-dd”
     *
     *
     * @return 格式化后的字符串
     */
    public static String format(Date date, String pattern) {
        return compileFormatter(pattern).format(date);
    }

    @SuppressWarnings("EnhancedSwitchMigration")
    public static Date parse(String text) {
        int match = strlen(text);
        switch (match) {
            case STRING_TEMP_DATE_MONTH: {
                return strhas(text, "-")
                        ? parse(text, "yyyy-MM")
                        : parse(text, "yyyy/MM");
            }

            case STRING_TEMP_DATE_DAY: {
                return strhas(text, "-")
                        ? parse(text, "yyyy-MM-dd")
                        : parse(text, "yyyy/MM/dd");
            }

            case STRING_TEMP_DATE_TIME: {
                return strhas(text, "-")
                        ? parse(text, "yyyy-MM-dd HH:mm:ss")
                        : parse(text, "yyyy/MM/dd HH:mm:ss");
            }

            default:
                throw new ValidationException("日期格式不正确");
        }
    }

    /**
     * #brief：根据解析规则将字符串对象转换为日期对象实例。<p>
     *
     * 根据解析规则将字符串对象转换为日期对象实例，通过 {@link SimpleDateFormat} 对象
     * 实现日期格字符串解析。使用 `new` 关键字创建 {@link SimpleDateFormat} 对象实例满
     * 足线程安全要求。
     *
     * @param text
     *        解析日期对象，如：“2018-12-18”
     *
     * @param pattern
     *        格式化规则，如：“yyyy-MM-dd”
     *
     * @return 解析后的字符串
     */
    public static Date parse(String text, String pattern) {
        return compileFormatter(pattern).parse(text);
    }

}
