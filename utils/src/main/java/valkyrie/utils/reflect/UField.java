package valkyrie.utils.reflect;

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

/* Creates on 2019/5/16. */

import valkyrie.utils.Assert;
import valkyrie.utils.Captor;
import valkyrie.utils.Optional;
import valkyrie.utils.string.StrStaticImports;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * `UField` 类封装了 Java 反射中的 `Field` 对象，提供对字段的访问和操作功能。
 *
 * <p>该类允许通过反射机制获取字段信息，读取和写入字段值，并支持属性的复制和注解检查等操作。
 *
 * <h2>主要功能</h2>
 * <ul>
 *     <li>封装了 `Field` 对象，用于访问字段的元数据和操作字段值。</li>
 *     <li>支持通过字段名称和类描述符查找字段。</li>
 *     <li>提供读取、写入字段值的方法，以及将字段值从一个对象复制到另一个对象的方法。</li>
 *     <li>支持检查字段上的注解。</li>
 * </ul>
 *
 * @author Luo Tiansheng
 * @since 1.0
 */
public class UField {
    /**
     * 成员属性对象
     */
    private final Field field;
    /**
     * 属性修饰符
     */
    private final int modifiers;
    /**
     * 属性所在类
     */
    private final Class<?> inClass;
    /**
     * 属性路径
     */
    private final String path;
    /**
     * 属性类型
     */
    private final Class<?> originType;
    /**
     * 成员名称
     */
    private final String name;

    public UField(String name, Class<?> inDescriptor) {
        this(findDescriptorField(name, inDescriptor));
    }

    /**
     * #brief：通过`Field`对象初始化`ObjectField`<p>
     *
     * 通过`Field`对象初始化`ObjectField`, 这个构造器会通过 {@code field} 参数
     * 获取属性的名称，类型等元数据信息。从而初始化`ObjectField`对象实例。
     *
     * @param field
     *        属性
     */
    public UField(Field field) {
        this.field = field;
        this.modifiers = field.getModifiers();
        this.inClass = field.getDeclaringClass();
        this.name = field.getName();
        this.path = StrStaticImports.fmt("%s#%s", inClass.getName(), name);
        this.originType = field.getType();
    }

    ///////////////////////////////////////////////////////////////////////////
    /// get
    ///////////////////////////////////////////////////////////////////////////

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    /**
     * 返回当前属性对象类型
     */
    public UClass getType() {
        return new UClass(originType);
    }

    /**
     * 返回当前属性原始对象类型
     */
    public Class<?> getOriginType() {
        return originType;
    }

    /**
     * 当前成员是否使用了`static`修饰
     */
    public boolean isStatic() {
        return (modifiers & Modifier.STATIC) > 0;
    }

    /**
     * 当前成员是否使用了`final`修饰
     */
    public boolean isFinal() {
        return (modifiers & Modifier.FINAL) > 0;
    }

    /** 属性数据读取接口 */
    interface FieldReader<T> { T get(); }
    /** 属性数据写入接口 */
    interface FieldWriter { void set(Object value); }

    /**
     * 可访问的`Field`对象实例，该实例默认`Field`对象的
     * `Accessible`为true；并且支持读写操作。
     */
    static class FieldDescriptor implements FieldReader<Object>, FieldWriter {
        private final Field field; /* 可访问的 field */
        private final Object instance;
        /**
         * 创建 Field
         */
        public FieldDescriptor(Field field, Object instance) {
            this.field = field;
            this.field.setAccessible(true);
            this.instance = instance;
        }
        /**
         * 获取当前属性中的值
         */
        public Object get() {
            return Captor.call(() -> field.get(instance));
        }
        /**
         * 设置当前属性中的值
         */
        public void set(Object value) {
            Captor.call(() -> field.set(instance, value));
        }
    }

    private FieldDescriptor createFieldDescriptor(Object obj) {
        return new FieldDescriptor(field, obj);
    }

    /**
     * 查找 {@code descriptor} 参数中是否存在 {@code name} 属性。这个
     * 函数会优先从当前 {@code descriptor} 类对象中查找属性;<p>
     *
     * 如果当前类中找不到 {@code name} 属性则会从父类一直往上查找。直到找不到为止。如果
     * 存在 {@code name} 属性则返回该属性 Field 对象。不存在则抛出异常。
     */
    static Field findDescriptorField(String name, Class<?> descriptor) {
        Field field = Optional.ifError(() -> descriptor.getDeclaredField(name),
                findDescriptorField0(name, descriptor));
        Assert.notNull(field, "属性 %s 在 %s 类中不存在", name, descriptor.getName());
        return field;
    }

    /** 递归从 {@code descriptor} 的父类查找 {@code name} 属性 */
    static Field findDescriptorField0(String name, Class<?> descriptor) {
        Field rfield;
        Class<?> superclass = descriptor.getSuperclass();

        /* 如果没有父类直接跳出该方法 */
        if (superclass == null)
            return null;

        if ((rfield = Optional.ifError(() -> superclass.getDeclaredField(name), null)) == null)
            rfield = findDescriptorField0(name, superclass);

        return rfield;
    }

    /**
     * #brief：根据属性名称写入对象属性数据<p>
     *
     * 根据属性名写入数据到指定属性中，通过反射获取属性`Field`并对这个属性
     * 对象进行反射赋值。不会经过`set`函数。
     *
     * @param instance
     *        对象实例
     */
    public Object get(Object instance) {
        return createFieldDescriptor(instance).get();
    }

    /**
     * #brief：根据属性名称写入对象属性数据<p>
     *
     * 根据属性名写入数据到指定属性中，通过反射获取属性`Field`并对这个属性
     * 对象进行反射赋值。不会经过`set`函数。
     *
     * @param value
     *        写入的数据
     *
     * @param instance
     *        对象实例
     */
    public void set(Object instance, Object value) {
        createFieldDescriptor(instance).set(value);
    }

    /**
     * #brief：将`src`实例中的当前属性对象拷贝到`dest`中<p>
     *
     * 将`src`实例中的当前属性对象拷贝到`dest`中，首先必须确保`src`对象中
     * 和`dest`对象中都存在当前`Field`属性。否则拷贝失败。
     *
     * @param src
     *        源对象实例
     *
     * @param dest
     *        目标对象实例
     */
    public static void copy(Object src, Object dest, String name) {
        /* read value of src. */
        Object value = new UField(name, src.getClass()).get(src);
        /* write to dest. */
        UField destUField = new UField(name, dest.getClass());
        destUField.set(dest, value);
    }

    /**
     * #brief：将`src`实例中的当前属性对象拷贝到`dest`中（字段不存在则忽略）<p>
     *
     * 将`src`实例中的当前属性对象拷贝到`dest`中，如果字段不存在则忽略掉
     * 不抛出异常。
     *
     * @param src
     *        源对象实例
     *
     * @param dest
     *        目标对象实例
     */
    public static void copyIgnoreError(Object src, Object dest, String name) {
        Captor.icall(() -> copy(src, dest, name));
    }

    /**
     * #brief：判断当前属性上是否存在 {@code annotation} 注解<p>
     *
     * 判断当前属性上是否存在 {@code annotation} 注解，如果存在传入的注解类则
     * 返回 {@code true}；否则不存在返回 {@code false}。
     *
     * @param annotation
     *        注解类
     *
     * @return 如果存在传入的注解类则返回 {@code true}；否则不存在
     *         则返回 {@code false}。
     */
    public boolean hasAnnotation(Class<? extends Annotation> annotation) {
        return getAnnotation(annotation) != null;
    }

    /**
     * #brief：获取属性上的注解信息<p>
     *
     * 获取属性上的注解信息，根据 {@code annotation} 参数获取当前属性上的注解。
     * 如果注解存在则返回注解对象，不存在则返回 {@code null}。
     *
     * @param annotation
     *        注解类
     *
     * @return 如果 {@code annotation} 被注解在当前属性上，那么就
     *         返回注解对象。如果不存在则返回 {@code null}。
     */
    public <T extends Annotation> T getAnnotation(Class<T> annotation) {
        T a = field.getDeclaredAnnotation(annotation);
        if (a == null)
            a = field.getAnnotation(annotation);
        return a;
    }

    public boolean typecheck(Class<?> aClass) {
        return aClass == getOriginType();
    }

    /**
     * 检查当前描述符是否为基本类型或包装类型。
     *
     * <p>该方法判断当前对象是否为原生基本类型或其对应的包装类型（如 `int` 或 {@link Integer}）。
     * 优先检查描述符是否为基本类型，随后判断是否为常见包装类型之一。
     *
     * @return 如果当前描述符是基本类型或包装类型，则返回 true；否则返回 false
     */
    public boolean isPrimitiveCheck() {
        return getType().isPrimitiveCheck();
    }

}
