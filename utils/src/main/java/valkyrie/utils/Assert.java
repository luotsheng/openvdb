package valkyrie.utils;

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
|*    Unless is by applicable law or agreed to in writing, software           *|
|*    distributed under the License is distributed on an "AS IS" BASIS,             *|
|*    WITHOUT WARRANTIES OR exprS OF ANY KIND, either express or implied.      *|
|*    See the License for the specific language governing permissions and           *|
|*    limitations under the License.                                                *|
|*                                                                                  *|
\* -------------------------------------------------------------------------------- */

import valkyrie.utils.collection.Lists;
import valkyrie.utils.collection.Maps;
import valkyrie.utils.exception.AssertException;
import valkyrie.utils.string.StrStaticImports;

import java.util.Collection;
import java.util.Map;

import static valkyrie.utils.TypeConverter.anyeq;

/**
 * 运行时断言工具类，这个类断言失败后并不会直接停止整个程序，而是会将所有失败的断言
 * 信息作为 Runtime 异常抛出。
 *
 * @author Luo Tiansheng
 */
public class Assert {

    /**
     * 如果条件为 true，则抛出断言异常。
     *
     * <p>此方法用于验证某个条件是否为 true，如果条件为 true，将抛出 {@link AssertException}。
     *
     * @param expr 要检查的条件
     * @throws AssertException 如果条件为 true
     */
    public static void isTrue(boolean expr) {
        isTrue(expr, "assert expr == true");
    }

    /**
     * 当条件为 true 时，抛出自定义的 {@link AssertException}。
     *
     * <p>此方法用于在条件不满足时触发异常，便于进行断言检查，确保程序的状态符合预期。
     *
     * @param expr 需要检查的条件
     * @param fmt 自定义异常信息的格式化字符串
     * @param args 格式化字符串的参数
     *
     * @throws AssertException 如果 {@code expr} 为 true，将抛出异常，并使用 {@code fmt} 和 {@code args}
     *                         格式化异常信息。
     */
    public static void isTrue(boolean expr, String fmt, Object... args) {
        if (!expr)
            throw new AssertException(fmt, args);
    }

    /**
     * 如果条件为 false，则抛出断言异常。
     *
     * <p>此方法用于验证某个条件是否为 false，如果条件为 false，将抛出 {@link AssertException}。
     *
     * @param expr 要检查的条件
     * @throws AssertException 如果条件为 false
     */
    public static void isFalse(boolean expr) {
        isFalse(expr, "assert expr == false");
    }

    /**
     * 当条件为 false 时，抛出自定义的 {@link AssertException}。
     *
     * <p>此方法用于在条件不满足时触发异常，便于进行断言检查，确保程序的状态符合预期。
     *
     * @param expr 需要检查的条件
     * @param fmt 自定义异常信息的格式化字符串
     * @param args 格式化字符串的参数
     *
     * @throws AssertException 如果 {@code expr} 为 false，将抛出异常，并使用 {@code fmt} 和 {@code args}
     *                         格式化异常信息。
     */
    public static void isFalse(boolean expr, String fmt, Object... args) {
        if (expr)
            throw new AssertException(fmt, args);
    }

    /**
     * 如果集合为空，则抛出断言异常。
     *
     * <p>此方法用于检查集合的状态，如果集合为空，将抛出 {@link AssertException}。
     *
     * @param collection 要检查的集合
     * @throws AssertException 如果集合为空
     */
    public static <E> void notEmpty(Collection<E> collection) {
        notEmpty(collection, "collection is empty!");
    }

    /**
     * 如果集合为空，则抛出断言异常。
     *
     * <p>此方法用于检查集合的状态，如果集合为空，将抛出 {@link AssertException}。
     *
     * @param collection 要检查的集合
     * @throws AssertException 如果集合为空
     */
    public static <E> void notEmpty(Collection<E> collection, String fmt, Object... args) {
        if (Lists.isEmpty(collection))
            throw new AssertException(fmt, args);
    }

    /**
     * 如果 Map 为空，则抛出断言异常。
     *
     * <p>此方法用于检查 Map 的状态，如果 Map 为空，将抛出 {@link AssertException}。
     *
     * @param map 要检查的 Map
     * @throws AssertException 如果 Map 为空
     */
    public static <K, V> void notEmpty(Map<K, V> map) {
        notEmpty(map, "map is empty!");
    }

    /**
     * 如果 Map 为空，则抛出断言异常。
     *
     * <p>此方法用于检查 Map 的状态，如果 Map 为空，将抛出 {@link AssertException}。
     *
     * @param map 要检查的 Map
     * @throws AssertException 如果 Map 为空
     */
    public static <K, V> void notEmpty(Map<K, V> map, String fmt, Object... args) {
        if (Maps.isEmpty(map))
            throw new AssertException(fmt, args);
    }

    /**
     * 如果字符串为空，则抛出断言异常。
     *
     * <p>此方法用于检查字符串的状态，如果字符串为空，将抛出 {@link AssertException}。
     *
     * @param source 要检查的字符串
     * @throws AssertException 如果字符串为 null 或空字符串
     */
    public static void notEmpty(String source) {
        notEmpty(source, "string is null or empty!");
    }

    /**
     * 如果字符串为空，则抛出断言异常。
     *
     * <p>此方法用于检查字符串的状态，如果字符串为空，将抛出 {@link AssertException}。
     *
     * @param source 要检查的字符串
     * @throws AssertException 如果字符串为 null 或空字符串
     */
    public static void notEmpty(String source, String fmt, Object... args) {
        if (StrStaticImports.strempty(source))
            throw new AssertException(fmt, args);
    }


    /**
     * 检查给定条件是否为 null，如果是，则抛出断言异常。
     *
     * <p>此方法用于验证一个对象是否为 null，如果为 null，将抛出 {@link AssertException}。
     *
     * @param expr 要检查的对象
     * @throws AssertException 如果条件为 null
     */
    public static void notNull(Object expr) {
        notNull(expr, "null");
    }

    /**
     * 检查给定条件是否为 null，如果是，则抛出断言异常，并使用自定义的异常信息。
     *
     * <p>此方法用于验证一个对象是否为 null，如果为 null，将抛出 {@link AssertException}，同时提供格式化支持。
     *
     * @param expr 要检查的对象
     * @param fmt 自定义异常信息的格式
     * @param args 格式化参数
     * @throws AssertException 如果条件为 null
     */
    public static void notNull(Object expr, String fmt, Object... args) {
        if (expr == null)
            throw new AssertException(fmt, args);
    }

    /**
     * 检查给定条件是否不为 null，如果是，则抛出断言异常。
     *
     * <p>此方法用于验证一个对象是否不为 null，如果不为 null，将抛出 {@link AssertException}。
     *
     * @param expr 要检查的对象
     * @throws AssertException 如果条件为 null
     */
    public static void isNull(Object expr) {
        isNull(expr, "not null");
    }

    /**
     * 检查给定条件是否不为 null，如果是，则抛出断言异常，并使用自定义的异常信息。
     *
     * <p>此方法用于验证一个对象是否不为 null，如果不为 null，将抛出 {@link AssertException}，同时提供格式化支持。
     *
     * @param expr 要检查的对象
     * @param fmt 自定义异常信息的格式
     * @param args 格式化参数
     * @throws AssertException 如果条件为 null
     */
    public static void isNull(Object expr, String fmt, Object... args) {
        if (expr != null)
            throw new AssertException(fmt, args);
    }

    /**
     * #brief: 断言两个对象不相等
     *
     * <p>该方法用于判断两个对象是否不相等。如果两个
     * 对象相等，则抛出 `AssertException`，提示断言
     * 失败；如果不相等，则正常结束。
     *
     * <p>该方法可用于单元测试或需要验证对象不相等的场景。
     *
     * @param actual 实际值
     * @param expected 期望值
     * @throws AssertException 当 `actual` 和 `expected` 相等时抛出异常
     */
    public static void notEquals(Object actual, Object expected) {
        notEquals(actual, expected, "assert actual == expected");
    }

    /**
     * #brief: 断言两个对象不相等
     *
     * <p>该方法用于判断两个对象是否不相等。如果两个
     * 对象相等，则抛出 `AssertException`，提示断言
     * 失败；如果不相等，则正常结束。
     *
     * <p>该方法可用于单元测试或需要验证对象不相等的场景。
     *
     * @param actual 实际值
     * @param expected 期望值
     * @param message 自定义异常信息
     * @param args 格式化参数
     * @throws AssertException 当 `actual` 和 `expected` 相等时抛出异常
     */
    public static void notEquals(Object actual, Object expected, String message, Object... args) {
        if (anyeq(actual, expected))
            throw new AssertException(message, args);
    }

    /**
     * #brief: 断言两个对象相等
     *
     * <p>该方法用于判断两个对象是否相等。如果两个
     * 对象不相等，则抛出 `AssertException`，提示断言
     * 失败；如果相等，则正常结束。
     *
     * <p>该方法可用于单元测试或需要验证对象相等的场景。
     *
     * @param actual 实际值
     * @param expected 期望值
     * @throws AssertException 当 `actual` 和 `expected` 相等时抛出异常
     */
    public static void isEquals(Object actual, Object expected) {
        isEquals(actual, expected, "assert actual != expected");
    }

    /**
     * #brief: 断言两个对象相等
     *
     * <p>该方法用于判断两个对象是否相等。如果两个
     * 对象不相等，则抛出 `AssertException`，提示断言
     * 失败；如果相等，则正常结束。
     *
     * <p>该方法可用于单元测试或需要验证对象相等的场景。
     *
     * @param actual 实际值
     * @param expected 期望值
     * @param message 自定义异常信息
     * @param args 格式化参数
     * @throws AssertException 当 `actual` 和 `expected` 不相等时抛出异常
     */
    public static void isEquals(Object actual, Object expected, String message, Object... args) {
        if (anyeq(actual, expected))
            throw new AssertException(message, args);
    }

    /**
     * #brief: 判断字符串对象是否为空对象
     *
     * @param text 文本内容
     * @throws AssertException 如果不存在文本就抛出异常
     */
    public static void hasText(String text) {
        hasText(text, "no text");
    }

    /**
     * #brief: 判断字符串对象是否为空对象
     *
     * @param text 文本内容
     * @param message 自定义异常信息
     * @param args 格式化参数
     * @throws AssertException 如果不存在文本就抛出异常
     */
    public static void hasText(String text, String message, Object... args) {
        if (StrStaticImports.strempty(text))
            throw new AssertException(message, args);
    }

}
