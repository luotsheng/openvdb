package valkyrie.utils.exception;

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

import valkyrie.utils.string.StrStaticImports;

/**
 * `SystemRuntimeException` 是一个自定义的运行时异常类，用于在系统中抛出各种运行时异常。
 * 该类继承自 `RuntimeException`，提供了多种构造函数以便根据不同的异常情况创建实例。<p>
 *
 * <h2>使用场景</h2>
 * <p>此异常类适用于在系统中抛出自定义异常，例如系统运行时错误、格式化消息的异常信息等。
 * 开发者可以通过不同的构造函数传递异常信息、异常原因以及格式化的错误消息。</p>
 *
 * <h2>构造函数</h2>
 * <ul>
 *     <li>{@link #SystemRuntimeException()} - 默认构造函数，不带任何参数。</li>
 *     <li>{@link #SystemRuntimeException(Throwable e)} - 通过已有的异常创建一个新的运行时异常。</li>
 *     <li>{@link #SystemRuntimeException(String fmt, Object... args)} - 根据格式化字符串和参数创建异常信息。</li>
 *     <li>{@link #SystemRuntimeException(String fmt, Throwable e, Object... args)} - 根据格式化字符串、已有异常和参数创建异常信息。</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>
 *     // 抛出一个简单的运行时异常
 *     throw new SystemRuntimeException("发生未知错误");
 *
 *     // 抛出一个包含其他异常的运行时异常
 *     try {
 *         // 代码逻辑
 *     } catch (Exception e) {
 *         throw new SystemRuntimeException(e);
 *     }
 *
 *     // 抛出一个格式化消息的运行时异常
 *     throw new SystemRuntimeException("错误代码：%d，描述：%s", errorCode, errorMessage);
 * </pre>
 *
 * @author Luo Tiansheng
 * @since 1.0
 */
public class SystemRuntimeException extends RuntimeException {

    /**
     * 默认构造函数。
     */
    public SystemRuntimeException() {
    }

    /**
     * 通过已有的异常创建一个新的运行时异常。
     *
     * @param e 原始异常，不能为 null。
     */
    public SystemRuntimeException(Throwable e) {
        super(e);
    }

    /**
     * 根据格式化字符串和参数创建异常信息。
     *
     * @param fmt 格式化字符串，不能为 null。
     * @param args 格式化参数。
     */
    public SystemRuntimeException(String fmt, Object... args) {
        super(StrStaticImports.fmt(fmt, args));
    }

    /**
     * 根据格式化字符串、已有异常和参数创建异常信息。
     *
     * @param fmt 格式化字符串，不能为 null。
     * @param e 原始异常，不能为 null。
     * @param args 格式化参数。
     */
    public SystemRuntimeException(String fmt, Throwable e, Object... args) {
        super(StrStaticImports.fmt(fmt, args), e);
    }

}

