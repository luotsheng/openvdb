package valkyrie.utils.reflect;

/* -------------------------------------------------------------------------------- *\
|*                                                                                  *|
|*    Copyright (C) 2023 RedGogh                                                       *|
|*                                                                                  *|
|*    This program is free software: you can redistribute it and/or modify          *|
|*    it under the terms of the GNU General Public License as published by          *|
|*    the Free Software Foundation, either version 3 of the License, or             *|
|*    (at your option) any later version.                                           *|
|*                                                                                  *|
|*    This program is distributed in the hope that it will be useful,               *|
|*    but WITHOUT ANY WARRANTY; without even the implied warranty of                *|
|*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                 *|
|*    GNU General Public License for more details.                                  *|
|*                                                                                  *|
|*    You should have received a copy of the GNU General Public License             *|
|*    along with this program.  If not, see <https://www.gnu.org/licenses/>.        *|
|*                                                                                  *|
|*    This program comes with ABSOLUTELY NO WARRANTY; for details type `show w'.    *|
|*    This is free software, and you are welcome to redistribute it                 *|
|*    under certain conditions; type `show c' for details.                          *|
|*                                                                                  *|
\* -------------------------------------------------------------------------------- */

/* Creates on 2019/5/16. */

import valkyrie.utils.Assert;
import valkyrie.utils.Captor;
import valkyrie.utils.TypeConverter;
import valkyrie.utils.collection.Lists;
import valkyrie.utils.exception.SystemRuntimeException;
import valkyrie.utils.stream.Streams;
import valkyrie.utils.string.StrStaticImports;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * `UClass` 是一个用于处理 Java 类元数据的工具类。它封装了一个 `Class` 对象，并提供了一些方法来
 * 访问和操作该类的属性和方法。可以通过该类实例化对象、获取类的属性列表以及调用静态方法等。
 *
 * <p>该类提供了静态和实例方法来创建类实例、获取属性、调用静态方法等操作。它支持通过构造函数
 * 参数实例化对象，并能处理包含可选参数的构造函数。
 *
 * <p>该类的主要特点包括：
 * <ul>
 *     <li>封装了 Java 的 `Class` 对象，提供对类的元数据的访问。</li>
 *     <li>能够根据类的构造函数参数实例化对象。</li>
 *     <li>提供获取类属性和调用静态方法的功能。</li>
 * </ul>
 *
 * <p>该工具类适用于需要动态操作 Java 类元数据的场景，如反射操作和动态实例化对象等。
 *
 * <h2>注意事项</h2>
 * <ul>
 *     <li>构造函数参数需要与类的构造函数匹配，否则会抛出异常。</li>
 *     <li>访问属性和调用静态方法时需要保证传入的名称正确，否则会抛出异常。</li>
 * </ul>
 *
 * @author Luo Tiansheng
 * @see Class
 * @see Constructor
 * @see Method
 * @since 1.0
 */
public class UClass {

    public static ClassLoader classLoader = UClass.class.getClassLoader();

    /**
     * #brief: 类描述符
     *
     * <p>该属性存储类的 {@link Class} 对象描述符。它被标记为 {@code transient}，
     * 以防止在序列化过程中被保存。
     */
    private transient final Class<?> descriptor;

    /**
     * #brief: 属性列表
     *
     * <p>存储类的属性，以属性名称作为键，属性对象 {@link UField} 作为值。
     */
    private final Map<String, UField> fields = new LinkedHashMap<>();

    /**
     * #brief: 构造器，使用对象实例初始化
     *
     * <p>通过传入对象实例的 {@link Class} 对象初始化 {@link UClass} 实例。
     *
     * @param instance 对象实例
     */
    public UClass(Object instance) {
        this(instance.getClass());
    }

    /**
     * #brief: 构造器，使用类描述符初始化
     *
     * <p>通过类的 {@link Class} 对象初始化 {@link UClass} 实例，并初始化属性列表。
     *
     * @param descriptor 类的 {@link Class} 对象
     */
    public UClass(Class<?> descriptor) {
        this.descriptor = descriptor;
        /* init */
        List<UField> declaredFields = scanDescriptorDeclaredFields(descriptor, Lists.newArrayList());
        for (UField field : declaredFields) {
            String name = field.getName();
            if (!fields.containsKey(name))
                fields.put(field.getName(), field);
        }
    }

    /**
     * #brief: 根据类名创建 UClass 实例
     *
     * <p>根据给定的类名返回一个 {@link UClass} 实例。如果类名无效，将会抛出异常。
     *
     * @param className 类名
     * @return {@link UClass} 实例
     */
    public static UClass forName(String className) {
        return new UClass(Captor.call(() -> Class.forName(className)));
    }

    ////////////////////////////////////////////////////////////////////////////
    // static
    ////////////////////////////////////////////////////////////////////////////

    /**
     * #brief: 根据字段名称查找并返回指定类的常量值
     *
     * <p>该方法通过反射查找指定类中的常量字段，并返回与提供的名称匹配的常量值。
     * 如果找到匹配的字段并且该字段是 `static` 且 `final`，则返回其值。
     * 如果没有找到匹配的常量字段，则返回 `null`。
     *
     * @param aClass 需要查找常量的类的 `Class` 对象
     * @param name 要查找的常量字段名称
     * @param <T> 常量值的类型
     * @return 与提供名称匹配的常量值，如果未找到则返回 `null`
     */
    @SuppressWarnings("unchecked")
    public static <T> T getConstant(Class<?> aClass, String name) {
        UClass uClass = new UClass(aClass);
        for (UField field : uClass.fields.values()) {
            if (field.isStatic() && field.isFinal() && StrStaticImports.streq(field.getName(), name))
                return (T) field.get(null);
        }
        return null;
    }

    /**
     * #brief: 实例化一个类对象，根据类的构造器传入参数数据
     *
     * <p>根据传入的构造器参数实例化类对象。如果使用空构造器，则不传入参数。
     *
     * @param descriptor 类对象
     * @param parameters 构造器参数，如果使用空构造器则不传
     * @return 类对象实例
     */
    public static <T> T newInstance(Class<T> descriptor, Object... parameters) {
        try {
            Class<?>[] parametersClassArray = toClassArray(parameters);
            Constructor<T> constructor = descriptor.getConstructor(parametersClassArray);
            constructor.setAccessible(true);
            return constructor.newInstance(parameters);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * #brief: 将参数数组转换成类型数组
     *
     * <p>将传入的参数数组转换为相应的 {@link Class} 类型数组。
     *
     * @param parameters 参数数组
     * @return 转换后的类型数组
     */
    public static Class<?>[] toClassArray(Object... parameters) {
        List<Class<?>> parametersClassList = Lists.newArrayList();
        for (Object parameter : parameters)
            parametersClassList.add(parameter.getClass());
        Class<?>[] parametersClassArray = new Class<?>[parametersClassList.size()];
        parametersClassList.toArray(parametersClassArray);
        return parametersClassArray;
    }

    ////////////////////////////////////////////////////////////////////////////
    // java class methods
    ////////////////////////////////////////////////////////////////////////////

    public String getName() {
        return descriptor.getName();
    }

    public String getSimpleName() {
        return descriptor.getSimpleName();
    }

    ////////////////////////////////////////////////////////////////////////////
    // methods
    ////////////////////////////////////////////////////////////////////////////

    /**
     * 过滤出符合指定条件的字段列表。
     *
     * <p>该方法接收一个 {@link Predicate} 对象作为参数，用于对字段列表进行过滤。
     * 返回一个包含符合指定条件的字段的列表。
     *
     * @param predicate 过滤条件
     * @return 符合指定条件的字段列表
     */
    public List<UField> filterDeclaredFields(Predicate<UField> predicate) {
        return Streams.filter(getDeclaredFields(), predicate);
    }

    /**
     * 检查类中是否存在指定的方法。
     *
     * <p>该方法通过方法名和参数类型检查指定类是否定义了对应的方法。如果找到匹配的方法，返回 `true`；
     * 如果没有找到，返回 `false`。如果遇到安全异常，则抛出 {@link SystemRuntimeException}。
     *
     * @param callMethod    方法名称
     * @param parameterTypes 方法的参数类型
     * @return 如果方法存在则返回 `true`，否则返回 `false`
     * @throws SystemRuntimeException 如果发生安全异常
     */
    public boolean hasMethod(String callMethod, Class<?>... parameterTypes) {
        try {
            descriptor.getDeclaredMethod(callMethod, parameterTypes);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        } catch (SecurityException e) {
            throw new SystemRuntimeException(e);
        }
    }

    /**
     * 检查指定的注解是否存在于描述符上。
     *
     * @param annotation 注解类型对应的 Class 对象
     * @return 如果注解存在则返回 true，否则返回 false
     */
    public boolean isAnnotationPresent(Class<? extends Annotation> annotation) {
        return descriptor.isAnnotationPresent(annotation);
    }

    /**
     * 返回描述符上声明的指定注解实例。
     *
     * @param <A> 注解类型
     * @param annotation 注解类型对应的 Class 对象
     * @return 如果注解存在则返回注解实例，否则返回 null
     */
    public <A extends Annotation> A getDeclaredAnnotation(Class<A> annotation) {
        return (A) descriptor.getDeclaredAnnotation(annotation);
    }

    /**
     * #brief: 实例化当前类对象，可选构造函数参数
     *
     * <p>实例化当前类对象，可选构造函数参数。通过 {@code args} 参数列表自动选择
     * 实例化对象的构造器。如果没有参数默认使用无参构造器初始化。
     *
     * @param args 构造器参数（可变
     * @return 实例化类过后的对象实例
     */
    @SuppressWarnings("unchecked")
    public <T> T newInstance(Object... args) {
        return (T) newInstance(descriptor, args);
    }

    /**
     * #brief：获取当前类下的指定属性对象<p>
     * <p>
     * 获取当前类下的指定属性对象，属性对象已经是封装好了的`UField`
     * 对象实例，可以直接使用。
     *
     * @param name 属性名称
     * @return 封装好的`UField`实例列表
     */
    public UField getDeclaredField(String name) {
        for (UField field : getDeclaredFields()) {
            if (StrStaticImports.streq(field.getName(), name))
                return field;
        }
        return null;
    }

    /**
     * #brief：获取当前类下的所有属性列表<p>
     * <p>
     * 获取当前类下的所有属性列表，属性列表已经是封装好了的`UField`
     * 对象实例，可以直接使用。
     *
     * @return 封装好的`UField`实例列表
     */
    public List<UField> getDeclaredFields() {
        return getDeclaredFields(true);
    }

    /**
     * #brief：获取当前类下的所有属性列表<p>
     * <p>
     * 获取当前类下的所有属性列表，的属性列表已经是封装好了的`UField`
     * 对象实例，可以直接使用。
     *
     * @return 封装好的`UField`实例列表
     */
    public List<UField> getDeclaredFields(boolean isFilter) {
        if (!isFilter)
            return Lists.newArrayList(fields.values());
        /* 筛选出修饰符非 static & final 的属性列表 */
        return Streams.filter(fields.values(), value -> (!value.isStatic() && !value.isFinal()));
    }

    /**
     * 递归查找属性列表
     */
    static List<UField> scanDescriptorDeclaredFields(Class<?> descriptor, List<UField> declaredFields) {
        Field[] fields = descriptor.getDeclaredFields();
        declaredFields.addAll(Lists.map(fields, UField::new));

        Class<?> superclass = descriptor.getSuperclass();
        if (superclass != null && superclass != Object.class)
            scanDescriptorDeclaredFields(superclass, declaredFields);

        return declaredFields;
    }

    /**
     * #brief: 读取指定名称的字段值（可能返回 null）
     *
     * <p>根据字段名称从当前实例中读取字段值。如果未找到该字段，返回 `null`。
     * 该方法提供了灵活性，允许在未找到字段的情况下继续执行。
     *
     * @param name 字段的名称
     * @param instance 要读取字段值的对象实例
     * @param <R> 字段值的类型
     * @return 指定字段的值，如果未找到字段，则返回 `null`
     */
    @SuppressWarnings("unchecked")
    public <R> R ireadFieldValue(String name, Object instance) {
        UField uField = fields.get(name);
        if (uField == null)
            return null;
        return (R) uField.get(instance);
    }

    /**
     * #brief: 读取指定名称的字段值（不允许为 null）
     *
     * <p>根据字段名称从当前实例中读取字段值。如果未找到该字段，将抛出异常。
     * 该方法确保字段存在，以避免潜在的 `null` 值处理。
     *
     * @param name 字段的名称
     * @param instance 要读取字段值的对象实例
     * @param <R> 字段值的类型
     * @return 指定字段的值
     * @throws IllegalArgumentException 如果未找到指定字段
     */
    @SuppressWarnings("unchecked")
    public <R> R readFieldValue(String name, Object instance) {
        UField uField = fields.get(name);
        Assert.notNull(fields.get(name), "未在 %s 类中找到 %s 属性。", getName(), name);
        return (R) uField.get(instance);
    }

    /**
     * 使用反射机制调用实例方法。
     * <p>
     * 此方法允许通过名称和参数调用任何实例方法。若目标方法是静态方法，
     * 应传递 null 作为 obj 参数。对于非静态方法，obj 参数必须是调用方法
     * 的实际对象实例。
     * <p>
     * 例如，如果目标类是 MyClass，有一个名为 myMethod 的实例方法，可以通过
     * 该方法进行调用：
     * <pre>
     *     MyClass instance = new MyClass();
     *     Object result = invoke(instance, "myMethod", arg1, arg2);
     * </pre>
     * 若目标方法是静态方法，则只需传入 null 作为 obj 参数：
     * <pre>
     *     Object result = staticInvoke("myStaticMethod", arg1, arg2);
     * </pre>
     *
     * @param obj  要调用方法的实例对象。如果要调用静态方法，则传入 null。
     *             对于实例方法，这个对象必须是调用方法的实际实例。
     * @param name 要调用的方法的名称。该名称必须与目标方法的名称完全一致，
     *             包括大小写。
     * @param args 方法的参数列表，使用可变参数形式传入。参数顺序和类型必须
     *             与目标方法的签名匹配。如果方法没有参数，则可以传入一个
     *             空的参数列表（即，`Object[] args` 为 `null` 或 `args` 为
     *             一个空数组）。
     * @return 方法调用的返回值。如果目标方法的返回类型是 `void`，则返回 `null`。
     */
    public Object invoke(Object obj, String name, Object... args) {
        return invoke0(obj, name, args);
    }

    /**
     * 使用反射机制调用静态方法。
     * <p>
     * 此方法允许通过名称和参数调用静态方法。若目标方法是实例方法，
     * 应传递目标对象作为 obj 参数。对于静态方法，obj 参数应为 null。
     * <p>
     * 例如，如果目标类是 MyClass，有一个名为 myStaticMethod 的静态方法，
     * 可以通过该方法进行调用：
     * <pre>
     *     Object result = staticInvoke("myStaticMethod", arg1, arg2);
     * </pre>
     *
     * @param name 要调用的静态方法的名称。该名称必须与目标静态方法的名称
     *             完全一致，包括大小写。
     * @param args 方法的参数列表，使用可变参数形式传入。参数顺序和类型必须
     *             与目标静态方法的签名匹配。如果方法没有参数，则可以传入一个
     *             空的参数列表（即，`Object[] args` 为 `null` 或 `args` 为
     *             一个空数组）。
     * @return 方法调用的返回值。如果目标方法的返回类型是 `void`，则返回 `null`。
     */
    public Object staticInvoke(String name, Object... args) {
        return invoke0(null, name, args);
    }

    /**
     * #brief：执行实际的反射方法调用。
     *
     * <p>该方法根据方法名和参数通过反射机制查找并调用目标方法。它会处理
     * 实例方法和静态方法的不同调用情境。通过设置方法的可访问性为 true，
     * 确保可以访问私有或受保护的方法。
     *
     * <p>如果方法的调用过程中发生异常（如方法找不到、方法不可访问等），
     * 则会抛出适当的反射异常。
     *
     * @param obj  要调用方法的实例对象。如果调用静态方法则传入 null。
     *             对于实例方法，这个对象必须是调用方法的实际实例。
     * @param name 要调用的方法的名称。该名称必须与目标方法的名称完全一致，
     *             包括大小写。
     * @param args 方法的参数列表，使用可变参数形式传入。参数顺序和类型必须
     *             与目标方法的签名匹配。如果方法没有参数，则可以传入一个
     *             空的参数列表（即，`Object[] args` 为 `null` 或 `args` 为
     *             一个空数组）。
     * @return 方法调用的返回值。如果目标方法的返回类型是 `void`，则返回 `null`。
     */
    private Object invoke0(Object obj, String name, Object... args) {
        return Captor.call(() -> {
            Method method = args == null ? descriptor.getDeclaredMethod(name) :
                    descriptor.getDeclaredMethod(name, toClassArray(args));
            method.setAccessible(true);
            return method.invoke(obj, args);
        });
    }

    /**
     * #brief: 获取类加载器
     *
     * <p>返回加载此类的类加载器。如果类由引导类加载器加载，则返回 {@code null}。
     *
     * @return 加载此类的类加载器
     * @throws SecurityException 如果安全管理器存在且调用者的类加载器与请求类的类加载器不同，
     *                           并且调用者没有 {@link RuntimePermission} 权限，则抛出此异常
     * @see ClassLoader
     * @see SecurityManager#checkPermission
     * @see RuntimePermission
     */
    @SuppressWarnings("removal")
    public ClassLoader getClassLoader() {
        return descriptor.getClassLoader();
    }

    public Class<?> getDescriptor() {
        return descriptor;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Class)
            return descriptor == obj;
        return super.equals(obj);
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
        if (descriptor.isPrimitive())
            return true;
        return TypeConverter.checkin(descriptor, Short.class, Integer.class, Long.class,
                Float.class, Double.class, Character.class, Boolean.class);
    }

    @Override
    public String toString() {
        return getName();
    }

}
