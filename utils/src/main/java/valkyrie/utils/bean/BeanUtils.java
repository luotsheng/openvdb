package valkyrie.utils.bean;

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

import valkyrie.utils.Captor;
import valkyrie.utils.reflect.UClass;
import valkyrie.utils.reflect.UField;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static valkyrie.utils.string.StrStaticImports.strcap;
import static valkyrie.utils.string.StrStaticImports.strhas;

/**
 * Bean工具类，方便实现对两个对象之间的属性拷贝，这属于浅拷贝。如果需要
 * 深拷贝，可以使用 JSON 工具类实现对象的深拷贝。
 *
 * @author Luo Tiansheng
 */
public class BeanUtils {

    /**
     * #brief: 实例化一个类对象，根据类的构造器传入参数数据
     *
     * <p>根据传入的构造器参数实例化类对象。如果使用空构造器，则不传入参数。
     *
     * @param aClass 类对象
     * @param parameters 构造器参数，如果使用空构造器则不传
     * @return 类对象实例
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<?> aClass, Object... parameters) {
        return (T) UClass.newInstance(aClass, parameters);
    }

    /**
     * 将 [src] 集合列表中的对象属性拷贝到新的 [T] 对象实例列表中。
     *
     * @param src        源对象实例
     * @param destClass  目标对象类 类型，通过这个类型创建新的对象实例集合
     * @param ignores    被忽略的属性名
     * @param <T>       目标对象类型
     * @return          新对象实例列表
     */
    public static <T> List<T> copyProperties(Collection<?> src, Class<T> destClass, String... ignores) {
        List<T> retvals = new ArrayList<>();
        for (Object any : src) {
            retvals.add(copyProperties(any, destClass, ignores));
        }
        return retvals;
    }

    /**
     * 创建 [destClass] 实例对象，然后将 [src] 中的数据浅拷贝到新的 [destClass]
     * 实例对象中。
     *
     * @param src        源对象实例
     * @param destClass  目标对象类 类型，通过这个类型创建对象实例
     * @param ignores    被忽略的属性名
     * @param <T>       目标对象类型
     * @return          新的对象实例
     */
    public static <T> T copyProperties(Object src, Class<T> destClass, String... ignores) {
        T instance = UClass.newInstance(destClass);
        copyProperties(src, instance, ignores);
        return instance;
    }

    /**
     * 将 [src] 对象中的属性拷贝到 [dst] 对象中。
     *
     * @param src    源对象实例
     * @param dst   目标对象实例
     * @param ignores 被忽略的属性名
     */
    public static void copyProperties(Object src, Object dst, String... ignores) {
        UClass dstClass = new UClass(dst);
        for (UField field : dstClass.getDeclaredFields()) {
            String name = field.getName();
            if (ignores.length > 0 && strhas(name, ignores))
                continue;
            Captor.icall(() -> copyValue(src, new UClass(src), dst, dstClass, field));
        }
    }

    /**
     * 将 [src] 对象中的属性拷贝到 [dst] 对象中。
     * <p>
     * 直接拷贝，不经过 GET/SET 方法
     *
     * @param src    源对象实例
     * @param dst   目标对象实例
     * @param ignores 被忽略的属性名
     */
    public static void directCopy(Object src, Object dst, String... ignores) {
        UClass srcClass = new UClass(dst);
        UClass dstClass = new UClass(dst);
        for (UField dstField : dstClass.getDeclaredFields()) {
            String name = dstField.getName();
            if (ignores.length > 0 && strhas(name, ignores))
                continue;
            Captor.icall(() -> dstField.set(dst, srcClass.readFieldValue(name, src)));
        }
    }

    /** 拷贝数据 */
    private static void copyValue(Object src, UClass srcClass, Object dst, UClass dstClass, UField dstField) {
        String setMethod = "set" + strcap(dstField.getName());
        String getMethod = "get" + strcap(dstField.getName());
        if (dstClass.hasMethod(setMethod, dstField.getOriginType())) {
            if (srcClass.hasMethod(getMethod)) {
                Object param = srcClass.invoke(src, getMethod);
                if (param != null)
                    dstClass.invoke(dst, setMethod, param);
            } else {
                UField srcField = srcClass.getDeclaredField(dstField.getName());
                if (srcField != null) {
                    Object param = srcField.get(src);
                    if (param != null)
                        dstClass.invoke(dst, setMethod, param);
                }
            }
        }
    }

}
