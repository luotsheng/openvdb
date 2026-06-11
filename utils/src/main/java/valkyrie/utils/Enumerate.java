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

/* Create on 2019/6/14 */

import valkyrie.utils.exception.InvalidArgumentException;
import valkyrie.utils.reflect.UClass;
import valkyrie.utils.string.StrStaticImports;

/**
 * `Enumerates` 是一个工具类，提供了获取和查找枚举值的方法。主要包括获取指定枚举类的所有
 * 枚举值和根据名称查找枚举值的功能。这些方法使用了反射机制来操作枚举类，以提供灵活的
 * 枚举值处理能力。
 *
 * <p>该类中的方法主要用于处理枚举类型的数据，可以用于动态查找枚举值，适用于需要在运行时
 * 处理枚举的场景。
 *
 * <h2>主要方法</h2>
 * <ul>
 *     <li>{@link #values(Class)}: 获取指定枚举类的所有枚举值。</li>
 *     <li>{@link #checkout(Class, String)}: 根据名称查找并返回指定枚举类的枚举值。</li>
 * </ul>
 *
 * <h2>注意事项</h2>
 * <ul>
 *     <li>使用了 `@SuppressWarnings("unchecked")` 注解来忽略未经检查的类型转换警告。</li>
 *     <li>方法 `checkout` 可能会抛出 `InvalidArgumentException` 异常，需要进行相应的异常处理。</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>
 *     // 获取枚举类 `Color` 的所有枚举值
 *     Color[] colors = Enumerates.values(Color.class);
 *
 *     // 根据名称查找 `Color` 枚举中的枚举值
 *     Color color = Enumerates.checkout(Color.class, "RED");
 * </pre>
 *
 * <p>完整的使用示例和测试用例可以参考项目的测试包下的相关测试类。
 *
 * @author Luo Tiansheng
 *
 * @see Enum
 * @see InvalidArgumentException
 * @since 1.0
 */
public class Enumerate {

    /**
     * #brief: 获取指定枚举类的所有枚举值
     *
     * <p>该方法通过反射机制获取枚举类的所有枚举值。需要传入枚举类的 `Class` 对象，并返回
     * 枚举值数组。由于使用了类型转换，因此标记了 `@SuppressWarnings("unchecked")` 注解以
     * 忽略未经检查的类型转换警告。
     *
     * @param enumClass 枚举类的 `Class` 对象
     * @param <E> 枚举类的类型参数
     * @return 枚举类中定义的所有枚举值的数组
     */
    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> E[] values(Class<? extends Enum<E>> enumClass) {
        return (E[]) new UClass(enumClass).staticInvoke("values");
    }

    /**
     * #brief: 根据名称查找并返回指定枚举类的枚举值
     *
     * <p>该方法查找枚举类中名称匹配的枚举值。如果找到与提供的名称匹配的枚举值，则返回该
     * 枚举值。如果没有找到，则抛出 `InvalidArgumentException` 异常，指明参数错误和常量不存在。
     *
     * <p>方法首先调用 `values` 方法获取所有枚举值，然后遍历这些枚举值，比较它们的名称与
     * 提供的名称。如果找到匹配项，则返回对应的枚举值。否则，抛出 `InvalidArgumentException` 异常。
     *
     * @param enumClass 枚举类的 `Class` 对象
     * @param name 要查找的枚举值名称
     * @param <E> 枚举类的类型参数
     * @return 与提供名称匹配的枚举值
     * @throws NullPointerException 如果提供的名称没有匹配的枚举值
     */
    public static <E extends Enum<E>> E checkout(Class<? extends Enum<E>> enumClass, String name) {
        E elem = find(enumClass, name);
        Assert.notNull(elem, "参数错误【%s】常量不存在！", name);
        return elem;
    }

    /**
     * #brief: 根据名称查找并返回指定枚举类的枚举值
     *
     * <p>该方法在指定的枚举类中查找与提供的名称匹配的枚举值。如果找到匹配的枚举值，
     * 则返回该枚举值；如果未找到匹配项，则返回 `null`。该方法利用反射机制获取枚举
     * 类的所有枚举值，并逐一比较名称。
     *
     * @param enumClass 枚举类的 `Class` 对象
     * @param name 要查找的枚举值名称
     * @param <E> 枚举类的类型参数
     * @return 与提供名称匹配的枚举值，如果没有匹配项则返回 `null`
     */
    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>> E find(Class<? extends Enum<E>> enumClass, String name) {
        Enum<E>[] values = values(enumClass);
        for (Enum<E> value : values)
            if (StrStaticImports.strieq(value.name(), name))
                return (E) value;
        return null;
    }

}
