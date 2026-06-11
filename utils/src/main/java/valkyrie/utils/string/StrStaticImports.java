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

import valkyrie.utils.Optional;
import valkyrie.utils.TypeConverter;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

import static valkyrie.utils.TypeConverter.atos;

/**
 * `STRSL(String static library)` 是一个工具类，提供了一系列针对字符串的操作方法。这些方法用于处理
 * 字符串的常见操作，如获取长度、大小写转换、字符串比较、格式化、查找、替换和分割等。
 *
 * <p>该类中的所有方法都是静态的，用户可以直接调用而无需实例化。其设计目的是为了提高
 * 字符串操作的简便性和可读性，避免冗长的代码。
 *
 * <p>本类的主要特点包括：
 * <ul>
 *     <li>提供多种字符串比较方法，包括区分大小写和不区分大小写的比较。</li>
 *     <li>支持字符串的格式化和替换操作，简化格式化字符串的过程。</li>
 *     <li>提供字符串的分割、修剪和去除换行符的功能。</li>
 *     <li>包含正则表达式的匹配功能，并支持正则表达式的编译缓存。</li>
 * </ul>
 *
 * <p>该工具类特别适合在需要频繁处理字符串的场景中使用，能有效减少代码的复杂性。<p>
 *
 * <h2>注意事项</h2>
 * <ul>
 *     <li>某些方法要求传入的字符串参数不能为空，否则会抛出 {@link NullPointerException} 异常。</li>
 *     <li>部分方法可能会修改输入字符串的格式，需谨慎使用。</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>
 *     // 获取字符串长度
 *     int length = StringUtils.strlen("example");
 *
 *     // 字符串转换为小写
 *     String lower = StringUtils.lowercase("EXAMPLE");
 *
 *     // 字符串比较
 *     boolean isEqual = StringUtils.streq("test", "TEST");
 * </pre>
 *
 * @author Luo Tiansheng
 * @since 1.0
 */
@SuppressWarnings("SpellCheckingInspection")
public class StrStaticImports
{
    private static final Map<String, Pattern> compiled = new WeakHashMap<>();

    private static final Map<String, PathMatcher> pathMatcherCache = new WeakHashMap<>();

    /**
     * 获取字符串的长度。
     *
     * <p>此方法用于计算并返回输入字符串的字符数。支持任意类型的对象，
     * 输入会被转换为字符串。
     *
     * @param wstr 要计算长度的字符串对象
     * @return 字符串的长度；如果输入为 null，则返回 0
     */
    public static int strlen(Object wstr) {
        return TypeConverter.atos(wstr).length();
    }

    /**
     * 检查给定的字符串是否为空。
     *
     * <p>此方法判断字符串是否为 {@code null} 或者长度为0。
     * 如果是，则返回 {@code true}；否则返回 {@code false}。
     *
     * <p>示例用法：
     * <pre>
     *      boolean isEmpty = strempty("example");
     * </pre>
     *
     * @param wstr 要检查的字符串对象。
     * @return 如果字符串为空，返回 {@code true}；否则返回 {@code false}。
     */
    public static boolean strempty(Object wstr) {
        return wstr == null || strlen(wstr) == 0;
    }

    /**
     * 检查给定的字符串是否非空。
     *
     * <p>此方法在内部调用 {@link #strempty(Object)} 方法，返回其结果的相反值。
     *
     * <p>示例用法：
     * <pre>
     *      boolean isNotEmpty = strnempty("example");
     * </pre>
     *
     * @param wstr 要检查的字符串对象。
     * @return 如果字符串非空，返回 {@code true}；否则返回 {@code false}。
     */
    public static boolean strnempty(Object wstr) {
        return !strempty(wstr);
    }


    /**
     * 将字符串转换为小写。
     *
     * <p>此方法接受一个字符串对象，并将其所有字符转换为小写形式，适用于
     * 大多数字符集。返回的新字符串可以用于不区分大小写的比较。
     *
     * @param wstr 要转换的字符串对象
     * @param iface 后处理选项
     * @return 转换后的小写字符串；如果输入为 null，则返回 null
     */
    public static String lowercase(Object wstr, StringInterface... iface) {
        return StringInterface.pipelineExecutor(atos(wstr, String::toLowerCase), iface);
    }

    /**
     * 将字符串转换为大写。
     *
     * <p>此方法接受一个字符串对象，并将其所有字符转换为大写形式，适用于
     * 大多数字符集。返回的新字符串可以用于不区分大小写的比较。
     *
     * @param wstr 要转换的字符串对象
     * @param iface 后处理选项
     * @return 转换后的大写字符串；如果输入为 null，则返回 null
     */
    public static String uppercase(Object wstr, StringInterface... iface) {
        return StringInterface.pipelineExecutor(atos(wstr, String::toUpperCase), iface);
    }


    /**
     * 将字符串的首字母大写，并根据指定的操作数组执行进一步的字符串处理。
     *
     * <p>该方法将传入的字符串的首字母转换为大写，并根据提供的操作数组（如修剪、转换大小写等）对字符串进行处理。
     * 通过 {@link StringInterface} 枚举支持链式操作。
     *
     * @param wstr      需要处理的字符串对象
     * @param iface 要应用的字符串操作数组
     * @return 处理后的字符串
     */
    public static String strcap(Object wstr, StringInterface... iface) {
        StringBuilder builder = new StringBuilder(TypeConverter.atos(wstr));
        builder.replace(0, 1, uppercase(builder.charAt(0)));
        return StringInterface.pipelineExecutor(TypeConverter.atos(builder), iface);
    }

    /**
     * 比较两个字符串是否相等（区分大小写）。
     *
     * <p>此方法用于判断两个字符串对象的内容是否完全相同，包括大小写。
     * 常用于需要严格匹配的场景。
     *
     * @param a 第一个字符串对象
     * @param b 第二个字符串对象
     * @return 如果两个字符串相等，则返回 true；否则返回 false
     */
    public static boolean streq(Object a, Object b) {
        return TypeConverter.anyeq(TypeConverter.atos(a), TypeConverter.atos(b));
    }

    /**
     * 比较两个字符串是否不相等（区分大小写）。
     *
     * <p>此方法是对 `streq` 方法的反向判断，用于快速确认两个字符串
     * 内容是否不相同。
     *
     * @param a 第一个字符串对象
     * @param b 第二个字符串对象
     * @return 如果两个字符串不相等，则返回 true；否则返回 false
     */
    public static boolean strne(Object a, Object b) {
        return !streq(a, b);
    }

    /**
     * 比较两个字符串是否相等（不区分大小写）。
     *
     * <p>此方法用于判断两个字符串对象的内容是否相同，忽略大小写差异。
     * 适用于需要宽松匹配的场景，如用户输入的验证。
     *
     * @param a 第一个字符串对象
     * @param b 第二个字符串对象
     * @return 如果两个字符串相等，则返回 true；否则返回 false
     */
    public static boolean strieq(Object a, Object b) {
        return TypeConverter.anyeq(uppercase(a), uppercase(b));
    }

    /**
     * 比较两个字符串是否不相等（不区分大小写）。
     *
     * <p>此方法是对 `strieq` 方法的反向判断，用于快速确认两个字符串
     * 内容是否不相同，忽略大小写。
     *
     * @param a 第一个字符串对象
     * @param b 第二个字符串对象
     * @return 如果两个字符串不相等，则返回 true；否则返回 false
     */
    public static boolean strine(Object a, Object b) {
        return !strieq(a, b);
    }

    /**
     * 格式化字符串。
     *
     * <p>此方法将输入字符串格式化为指定的格式，支持多种参数，适用于
     * 动态生成文本或消息。
     *
     * @param wstr 要格式化的字符串对象
     * @param args 格式化所需的参数列表
     * @return 格式化后的字符串
     */
    public static String fmt(Object wstr, Object... args) {
        if (args == null || args.length == 0)
            return atos(wstr);
        return String.format(TypeConverter.atos(wstr), args);
    }

    /**
     * #brief: 检查字符串是否包含子串
     *
     * <p>将 `wstr` 转换为字符串后，检查它是否包含子串 `cmp`。
     *
     * @param wstr 任意对象，会被转换为字符串
     * @param cmp 要检查的子串
     * @return 如果 `wstr` 包含 `cmp`，返回 true；否则返回 false
     */
    public static boolean strhas(Object wstr, String cmp) {
        return TypeConverter.atos(wstr).contains(cmp);
    }

    /**
     * 检查字符串是否在指定的列表中。
     *
     * <p>此方法用于确定给定字符串是否存在于多个候选字符串中，适用于
     * 验证用户输入或查找。
     *
     * @param wstr 要检查的字符串对象
     * @param list 要检查的字符串列表
     * @return 如果存在则返回 true，否则返回 false
     */
    public static boolean strhas(Object wstr, Object... list) {
        if (list == null)
            return false;

        for (Object str : list) {
            if (streq(wstr, str))
                return true;
        }

        return false;
    }

    /**
     * 检查字符串是否在指定的字符串数组中。
     *
     * <p>此方法是 `strclude` 的重载，专门用于字符串数组的查找。
     *
     * @param wstr 要检查的字符串对象
     * @param list 要检查的字符串数组
     * @return 如果存在则返回 true，否则返回 false
     */
    public static boolean strhas(Object wstr, String... list) {
        return strhas(wstr, (Object[]) list);
    }

    /**
     * 检查字符串是否在指定的字符串集合中。
     *
     * <p>此方法是 `strclude` 的重载，专门用于集合的查找。
     *
     * @param wstr 要检查的字符串对象
     * @param list 要检查的字符串集合
     * @return 如果存在则返回 true，否则返回 false
     */
    public static boolean strhas(Object wstr, Collection<String> list) {
        return strhas(wstr, list.toArray());
    }

    /**
     * #brief: 忽略大小写检查字符串是否包含子串
     *
     * <p>将 `wstr` 和 `cmp` 都转换为大写后，检查它们之间是否有包含关系。
     *
     * @param wstr 任意对象，会被转换为字符串
     * @param cmp 要检查的子串
     * @return 忽略大小写后，如果 `wstr` 包含 `cmp`，返回 true；否则返回 false
     */
    public static boolean strihas(Object wstr, String cmp) {
        return strhas(uppercase(wstr), uppercase(cmp));
    }

    /**
     * 检查字符串（不区分大小写）是否在指定的列表中。
     *
     * <p>此方法用于判断给定字符串是否在多个候选字符串中，忽略大小写。
     *
     * @param wstr 要检查的字符串对象
     * @param list 要检查的字符串列表
     * @return 如果存在则返回 true，否则返回 false
     */
    public static boolean strihas(Object wstr, Object... list) {
        if (list == null)
            return false;

        for (Object str : list)
            if (streq(uppercase(wstr), uppercase(str)))
                return true;

        return false;
    }

    /**
     * 检查字符串（不区分大小写）是否在指定的字符串数组中。
     *
     * <p>此方法是 `striclude` 的重载，专门用于字符串数组的查找。
     *
     * @param wstr 要检查的字符串对象
     * @param list 要检查的字符串数组
     * @return 如果存在则返回 true，否则返回 false
     */
    public static boolean strihas(Object wstr, String... list) {
        return strihas(wstr, (Object[]) list);
    }

    /**
     * 检查字符串（不区分大小写）是否在指定的字符串集合中。
     *
     * <p>此方法是 `striclude` 的重载，专门用于集合的查找。
     *
     * @param wstr 要检查的字符串对象
     * @param list 要检查的字符串集合
     * @return 如果存在则返回 true，否则返回 false
     */
    public static boolean strihas(Object wstr, Collection<String> list) {
        return strihas(wstr, list.toArray());
    }

    /**
     * 使用正则表达式替换字符串中的内容。
     *
     * <p>此方法根据给定的正则表达式，查找并替换输入字符串中的所有匹配项。
     * 适用于需要对字符串进行复杂替换的场景。
     *
     * @param wstr 要处理的字符串对象
     * @param regexp 用于匹配的正则表达式
     * @param value 用于替换的字符串
     * @param iface 后处理选项
     * @return 替换后的字符串
     */
    public static String strrexp(Object wstr, String regexp, String value, StringInterface... iface) {
        return StringInterface.pipelineExecutor(TypeConverter.atos(wstr).replaceAll(regexp, value), iface);
    }

    /**
     * 按分隔符将字符串拆分为数组。
     *
     * <p>此方法根据指定的分隔符，将输入字符串分割成多个子字符串，并返回一个字符串数组。
     * 适用于解析以特定格式组织的数据。
     *
     * @param wstr 要拆分的字符串对象
     * @param delim 用于拆分的分隔符
     * @return 拆分后的字符串数组
     */
    public static String[] strtok(Object wstr, String delim, StringInterface... iface) {
        String[] origins = TypeConverter.atos(wstr).split(delim);

        if (iface != null) {
            for (int i = 0; i < origins.length; i++) {
                origins[i] = StringInterface.pipelineExecutor(origins[i], iface);
            }
        }

        return origins;
    }

    /**
     * 截取字符串的指定部分。
     *
     * <p>此方法根据给定的偏移量和长度，从输入字符串中提取出一个子字符串。
     * 适用于需要对字符串进行部分操作的场景。
     *
     * @param wstr 要处理的字符串对象
     * @param off 截取的起始位置
     * @param len 截取的长度
     * @return 截取后的字符串
     */
    public static String strcut(Object wstr, int off, int len, StringInterface... iface) {
        return StringInterface.pipelineExecutor(TypeConverter.atos(wstr, off, len), iface);
    }

    /**
     * #brief: 查找字符串中倒数第 N 次出现的子串位置
     *
     * <p>从字符串末尾开始反向查找指定子串，返回倒数第 {@code n} 次出现的位置索引。
     * 如果子串不存在或出现次数不足 {@code n} 次，则返回 {@code -1}。
     *
     * <pre>
     * strrstr("/a/b/c/d", "/", 1) = 6
     * strrstr("/a/b/c/d", "/", 2) = 4
     * strrstr("/a/b/c/d", "/", 3) = 2
     * strrstr("/a/b/c/d", "/", 4) = 0
     * strrstr("/a/b/c/d", "/", 5) = -1
     * </pre>
     *
     * @param str 待搜索的字符串
     * @param substr 要查找的子串
     * @param n 倒数第几次出现，从 1 开始
     * @return 倒数第 {@code n} 次出现的索引位置，不存在返回 {@code -1}
     */
    public static int strrstr(String str, String substr, int n)
    {
        int pos = str.length();

        for (int i = 0; i < n; i++) {
            pos = str.lastIndexOf(substr, pos - 1);

            if (pos < 0)
                return -1;
        }

        return pos;
    }

    /**
     * 统计字符串中的行数。
     *
     * <p>此方法通过遍历字符串并统计换行符（'\n'）的数量来计算文本的行数。
     * 该实现仅扫描字符数组，不使用正则表达式或字符串分割，
     * 适用于需要高性能统计文本行数的场景。
     *
     * <p>换行规则：
     * 每出现一个 '\n' 视为一行结束，因此返回值等于 '\n' 的数量。
     * 该方法同时兼容 Unix（\n）与 Windows（\r\n）换行格式，
     * 因为 Windows 换行同样包含 '\n'。
     *
     * @param wstr 要处理的字符串对象
     * @return 字符串中包含的行数（即 '\n' 的数量）
     */
    public static int strlines(Object wstr)
    {
        String text = atos(wstr);

        int lines = 0;

        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n')
                lines++;
        }

        return lines;
    }

    /**
     * 计算字符串中每一行文本的最大宽度。
     *
     * <p>此方法遍历字符串并统计每行字符数量，返回最长一行的宽度。
     * 行分隔符使用 '\n' 判断，兼容 Unix（\n）与 Windows（\r\n）换行格式。
     * 实现仅进行一次线性扫描，不使用正则表达式或字符串拆分，
     * 适用于需要高性能统计文本布局宽度的场景。
     *
     * @param wstr 要处理的字符串对象
     * @return 所有行中最长一行的字符数量
     */
    public static int strwidth(Object wstr)
    {
        String text = atos(wstr);

        int width = 0;
        int max = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '\n') {
                if (width > max)
                    max = width;

                width = 0;
            } else {
                width++;
            }
        }

        if (width > max)
            max = width;

        return max;
    }

    /**
     * 判断字符串是否仅包含 ASCII 字符。
     *
     * <p>此方法遍历字符串字符编码，检测是否存在大于 127 的字符。
     * 如果存在则返回 false，表示字符串包含非 ASCII（Unicode）字符。
     *
     * @param wstr 要检测的字符串对象
     * @return 如果字符串全部为 ASCII 字符返回 true，否则返回 false
     */
    public static boolean strascii(Object wstr)
    {
        String text = atos(wstr);

        for (int i = 0; i < text.length(); i++)
        {
            if (text.charAt(i) > 127)
                return false;
        }

        return true;
    }

    /**
     * 检查字符串是否为数字。
     *
     * <p>此方法用于判断输入字符串是否可以解析为有效的数字，适用于
     * 输入验证或数据解析。
     *
     * @param wstr 要检查的字符串对象
     * @return 如果字符串是有效的数字，则返回 true；否则返回 false
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean strdig(Object wstr) {
        return Optional.ifError(() -> Double.parseDouble(TypeConverter.atos(wstr)), true, false);
    }

    /**
     * 检查字符串是否与正则表达式匹配。(忽略大小写)
     *
     * <p>此方法用于确定输入字符串是否符合指定的正则表达式规则。
     * 常用于格式验证。
     *
     * @param obj 要检查的字符串对象
     * @param regexp 用于匹配的正则表达式
     * @return 如果字符串匹配则返回 true；否则返回 false
     */
    public static boolean strimatch(Object obj, String regexp) {
        return strmatch(lowercase(obj), lowercase(regexp));
    }

    /**
     * 检查字符串是否与正则表达式匹配。
     *
     * <p>此方法用于确定输入字符串是否符合指定的正则表达式规则。
     * 常用于格式验证。
     *
     * @param obj 要检查的字符串对象
     * @param regexp 用于匹配的正则表达式
     * @return 如果字符串匹配则返回 true；否则返回 false
     */
    public static boolean strmatch(Object obj, String regexp) {
        return strmatchepc(obj, regexp, false);
    }

    /**
     * 检查字符串是否与正则表达式匹配（支持缓存）。
     *
     * <p>此方法用于判断输入字符串是否符合指定的正则表达式规则，并支持
     * 缓存已编译的正则表达式以提高性能。
     *
     * @param obj 要检查的字符串对象
     * @param regexp 用于匹配的正则表达式
     * @return 如果字符串匹配则返回 true；否则返回 false
     */
    public static boolean strxmatch(Object obj, String regexp) {
        return strmatchepc(obj, regexp, true);
    }

    /**
     * 缓存和编译正则表达式。
     *
     * <p>此方法用于检查并缓存正则表达式，如果未缓存，则进行编译并保存。
     * 用于提高正则表达式的重用性能。
     *
     * @param regexp 要编译的正则表达式
     * @return 编译后的 Pattern 对象
     */
    private static Pattern _patternCacheComputeIfAbsent(String regexp) {
        return compiled.computeIfAbsent(regexp, k -> Pattern.compile(regexp));
    }

    /**
     * 根据正则表达式检查字符串匹配情况。
     *
     * <p>此方法内部实现用于确定输入字符串是否符合指定的正则表达式规则。
     * 可以选择是否使用缓存的正则表达式。
     *
     * @param obj 要检查的字符串对象
     * @param regexp 用于匹配的正则表达式
     * @param enablePatternCache 是否启用正则表达式缓存
     * @return 如果字符串匹配则返回 true；否则返回 false
     */
    private static boolean strmatchepc(Object obj, String regexp, boolean enablePatternCache) {
        Pattern pattern = enablePatternCache ? _patternCacheComputeIfAbsent(regexp) : Pattern.compile(regexp);
        assert pattern != null;
        return pattern.matcher(TypeConverter.atos(obj)).find();
    }

    /**
     * #brief: 检查给定字符串是否符合指定模式
     *
     * <p>该方法使用路径匹配器判断提供的字符串是否与
     * 指定的模式匹配。模式使用 glob 语法，可以实现
     * 简单的通配符匹配。
     *
     * <p>如果模式在缓存中不存在，将会创建一个新的
     * PathMatcher 并缓存，以提高后续匹配的效率。
     *
     * @param wstr 要匹配的字符串
     * @param pattern 匹配模式
     * @return 如果字符串符合模式，返回 true；否则返回 false
     */
    public static boolean strant(Object wstr, String pattern) {
        PathMatcher pathMatcher =
                pathMatcherCache.computeIfAbsent(pattern, k -> compilePathMatcher(pattern));
        return pathMatcher.matches(Paths.get(TypeConverter.atos(wstr)));
    }

    private static PathMatcher compilePathMatcher(String pattern) {
        return FileSystems.getDefault().getPathMatcher("glob:" + pattern);
    }

    /**
     * 移除字符串中的换行符。
     *
     * <p>此方法用于将输入字符串中的所有换行符（包括回车和换行）替换为空字符串。
     * 适用于需要清理多行文本的场景。
     *
     * @param wstr 要处理的字符串对象
     * @return 移除换行符后的字符串
     */
    public static String strip(Object wstr) {
        return TypeConverter.atos(wstr).trim().replaceAll("[\r\n]+", "");
    }

    /**
     * 将下划线风格的字符串转换为驼峰风格。
     *
     * <p>此方法用于将输入字符串的下划线分隔格式转换为驼峰命名格式。
     * 适用于编码风格的转换或数据格式的规范化。
     *
     * @param obj 要处理的字符串对象
     * @return 转换为驼峰风格后的字符串
     */
    public static String strlinehmp(Object obj, StringInterface... iface) {
        char[] charArray = TypeConverter.atos(obj).toCharArray();
        StringBuilder buffer = new StringBuilder();

        boolean next = false;
        for (char c : charArray) {
            char append = c;
            if (!next && append == '_') {
                next = true;
                continue;
            }

            if (next) {
                append = uppercase(c).charAt(0);
                next = false;
            }

            buffer.append(append);
        }

        return StringInterface.pipelineExecutor(buffer.toString(), iface);
    }

}
