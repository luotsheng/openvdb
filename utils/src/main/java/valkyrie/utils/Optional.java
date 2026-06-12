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
|*    Unless required by applicable law or agreed to in writing, software           *|
|*    distributed under the License is distributed on an "AS IS" BASIS,             *|
|*    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.      *|
|*    See the License for the specific language governing permissions and           *|
|*    limitations under the License.                                                *|
|*                                                                                  *|
\* -------------------------------------------------------------------------------- */

/* Creates on 2023/4/29. */

import valkyrie.utils.iface.ActionFunction;
import valkyrie.utils.iface.CallableFunction;
import valkyrie.utils.string.StrStaticImports;

/**
 * @author Luo Tiansheng
 */
public class Optional {

    /**
     * 如果给定的值为 null，则返回指定的替代值；否则返回原值。
     *
     * <p>此方法用于简化 null 检查，可以避免冗长的条件语句。
     *
     * @param value 要检查的值
     * @param orNull 如果 {@code value} 为 null 时返回的替代值
     * @param <T> 值的类型
     * @return 如果 {@code value} 为 null，则返回 {@code orNull}，否则返回 {@code value}
     */
    public static <T> T ifNullable(T value, T orNull) {
        return value == null ? orNull : value;
    }

    /**
     * 如果给定的值为 null，则返回指定的替代值；否则返回原值。
     *
     * <p>此方法用于简化 null 检查，可以避免冗长的条件语句。
     *
     * @param value 要检查的值
     * @param orNull 如果 {@code value} 为 null 时返回的替代值
     * @param <T> 值的类型
     * @return 如果 {@code value} 为 null，则返回 {@code orNull}，否则返回 {@code value}
     */
    public static <T> T ifNullable(T value, CallableFunction<T> orNull) {
        return Captor.call(() -> value == null ? orNull.call() : value);
    }

    /**
     * 如果给定的字符串值为 null 或空字符串，则返回指定的替代值；否则返回原值。
     *
     * <p>此方法用于简化空字符串检查，可以避免冗长的条件语句。
     *
     * @param value 要检查的值
     * @param orBlank 如果 {@code value} 为 null 时返回的替代值
     * @return 如果 {@code value} 为 null，则返回 {@code orBlank}，否则返回 {@code value}
     */
    public static String ifBlank(String value, String orBlank) {
        return StrStaticImports.strempty(value) ? orBlank : value;
    }

    /**
     * 执行给定的 {@link ActionFunction}，如果没有抛出异常，则返回 {@code orSuccess}，
     * 否则返回 {@code orError}。
     *
     * <p>此方法用于捕获可能抛出的异常，简化异常处理流程。成功执行时返回成功值，
     * 失败时返回预定义的错误值，避免程序中断。
     *
     * @param function 需要执行的函数，可能抛出异常
     * @param orSuccess 函数成功执行时返回的值
     * @param orError 函数执行失败时返回的值
     *
     * @return {@code orSuccess} 如果函数成功执行，{@code orError} 如果函数抛出异常
     */
    public static <T> T ifError(ActionFunction function, T orSuccess, T orError) {
        try {
            function.call();
            return orSuccess;
        } catch (Throwable e) {
            return orError;
        }
    }

    /**
     * 执行指定的函数，如果发生异常则返回指定的替代值。
     *
     * <p>此方法用于简化错误处理，可以在出现异常时返回一个默认值。
     *
     * @param function 要执行的函数
     * @param orError 如果函数执行时发生异常，则返回的替代值
     * @param <T> 返回值的类型
     * @return 函数执行的返回值；如果发生异常，则返回 {@code orError}
     */
    public static <T> T ifError(CallableFunction<T> function, T orError) {
        try {
            return function.call();
        } catch (Throwable e) {
            return orError;
        }
    }

}
