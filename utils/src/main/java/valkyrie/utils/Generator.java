package valkyrie.utils;

import valkyrie.utils.security.Codec;
import valkyrie.utils.string.StrStaticImports;
import valkyrie.utils.string.StringInterface;

import java.util.Random;
import java.util.UUID;

/**
 * `Generator` 是一个用于生成随机数的工具类，提供了多种生成随机整数、浮点数和双精度数的方法。
 *
 * <p>该类包含静态方法，用于生成指定范围内的随机数。这些方法适用于需要生成随机数的场景，如
 * 随机化算法、模拟测试、数据生成等。
 *
 * <h2>主要功能</h2>
 * <ul>
 *     <li>生成指定范围内的随机整数。</li>
 *     <li>生成指定范围内的随机浮点数。</li>
 *     <li>生成指定范围内的随机双精度数。</li>
 * </ul>
 *
 * @author Luo Tiansheng
 * @since 1.0
 */
public class Generator {

    /**
     * 包含字母的静态常量数组。
     */
    private static final char[] LETTER_ARRAY = new char[26];

    /**
     * 包含数字的静态常量数组。
     */
    private static final char[] NUMBER_ARRAY = new char[10];

    static {
        for (int i = 0; i < LETTER_ARRAY.length; i++)
            LETTER_ARRAY[i] = (char) ('a' + i);

        for (int i = 0; i < NUMBER_ARRAY.length; i++)
            NUMBER_ARRAY[i] = (char) ('0' + i);
    }

    /**
     * #brief: 生成一个不带符号的 UUID 字符串。<p>
     *
     * 该方法生成一个不带任何符号的 UUID 字符串实例，默认返回大写的
     * UUID 字符串对象。并且支持字符串扩展接口调用。
     * <p>
     * 如果希望生成小写的 UUID 字符串可以使用以下方式：
     * <pre>
     * Generator.uuid(STRING_IFACE_LOWER_CASE_EXT);
     * </pre>
     *
     * @param iface 字符串扩展接口
     * @return 生成不带符号的UUID
     */
    public static String uuid(StringInterface... iface) {
        return StrStaticImports.uppercase(UUID.randomUUID(), iface)
                .replace("-", "");
    }

    /**
     * 随机从 uuid 字符串中取 {@code n} 个字符组合返回。
     *
     * @param n
     *        随机从 uuid 中取 {@code number} 位字符
     *
     * @return 返回没有任何符号的 UUID
     */
    public static String uuid(int n, StringInterface... iface) {
        return StrStaticImports.strcut(uuid(iface), 0, n);
    }

    /**
     * #brief: 生成指定长度的随机代码
     *
     * <p>该方法生成一个随机长度的字符串，字符串长度由
     * 参数指定，内容为字母和数字的组合。调用该方法将
     * 默认生成从 0 到指定长度之间的随机长度代码。
     *
     * @param length 要生成的字符串的最大长度
     * @return 生成的随机代码
     */
    public static String randomCode(int length) {
        return randomCode(0, length, true);
    }

    /**
     * #brief: 生成指定范围内随机长度的代码
     *
     * <p>该方法生成一个随机长度的字符串，长度在指定的
     * 最小值和最大值之间，内容为字母和数字的组合。
     *
     * @param min 最小长度
     * @param max 最大长度
     * @return 生成的随机代码
     */
    public static String randomCode(int min, int max) {
        return randomCode(min, max, false);
    }

    /**
     * #brief: 生成指定范围内随机长度的代码
     *
     * <p>该方法生成一个随机长度的字符串，长度在指定的
     * 最小值和最大值之间，内容为字母和数字的组合。如果
     * `enableMaxLength` 为 true，则生成的长度将始终为
     * `max`。
     *
     * @param min 最小长度
     * @param max 最大长度
     * @param enableMaxLength 是否使用最大长度作为随机长度
     * @return 生成的随机代码
     */
    private static String randomCode(int min, int max, boolean enableMaxLength) {
        StringBuilder builder = new StringBuilder();
        int loopCount = enableMaxLength ? max : randomInt(min, max);
        for (int i = 0; i < loopCount; i++)
            builder.append(randomBoolean() ? LETTER_ARRAY[randomInt(LETTER_ARRAY.length - 1)] :
                    NUMBER_ARRAY[randomInt(NUMBER_ARRAY.length - 1)]);
        return builder.toString();
    }

    /**
     * #brief: 生成指定长度的随机字母代码
     *
     * <p>该方法生成一个随机长度的字符串，字符串内容仅
     * 包含字母。调用该方法将默认生成从 0 到指定长度之间
     * 的随机长度代码。
     *
     * @param length 要生成的字符串的最大长度
     * @return 生成的随机字母代码
     */
    public static String randomLetterCode(int length) {
        return randomLetterCode(0, length, true);
    }

    /**
     * #brief: 生成指定范围内随机长度的字母代码
     *
     * <p>该方法生成一个随机长度的字符串，长度在指定的
     * 最小值和最大值之间，字符串内容仅包含字母。
     *
     * @param min 最小长度
     * @param max 最大长度
     * @return 生成的随机字母代码
     */
    public static String randomLetterCode(int min, int max) {
        return randomLetterCode(min, max, false);
    }

    /**
     * #brief: 生成指定范围内随机长度的字母代码
     *
     * <p>该方法生成一个随机长度的字符串，长度在指定的
     * 最小值和最大值之间，字符串内容仅包含字母。如果
     * `enableMaxLength` 为 true，则生成的长度将始终为
     * `max`。
     *
     * @param min 最小长度
     * @param max 最大长度
     * @param enableMaxLength 是否使用最大长度作为随机长度
     * @return 生成的随机字母代码
     */
    private static String randomLetterCode(int min, int max, boolean enableMaxLength) {
        StringBuilder builder = new StringBuilder();
        int loopCount = enableMaxLength ? max : randomInt(min, max);
        for (int i = 0; i < loopCount; i++)
            builder.append(LETTER_ARRAY[randomInt(LETTER_ARRAY.length - 1)]);
        return builder.toString();
    }

    /**
     * #brief: 生成指定长度的随机数字代码
     *
     * <p>该方法生成一个随机长度的字符串，字符串内容仅
     * 包含数字。调用该方法将默认生成从 0 到指定长度之间
     * 的随机长度代码。
     *
     * @param length 要生成的字符串的最大长度
     * @return 生成的随机数字代码
     */
    public static String randomNumberCode(int length) {
        return randomNumberCode(0, length, true);
    }

    /**
     * #brief: 生成指定范围内随机长度的数字代码
     *
     * <p>该方法生成一个随机长度的字符串，长度在指定的
     * 最小值和最大值之间，字符串内容仅包含数字。
     *
     * @param min 最小长度
     * @param max 最大长度
     * @return 生成的随机数字代码
     */
    public static String randomNumberCode(int min, int max) {
        return randomNumberCode(min, max, false);
    }

    /**
     * #brief: 生成指定范围内随机长度的数字代码
     *
     * <p>该方法生成一个随机长度的字符串，长度在指定的
     * 最小值和最大值之间，字符串内容仅包含数字。如果
     * `enableMaxLength` 为 true，则生成的长度将始终为
     * `max`。
     *
     * @param min 最小长度
     * @param max 最大长度
     * @param enableMaxLength 是否使用最大长度作为随机长度
     * @return 生成的随机数字代码
     */
    private static String randomNumberCode(int min, int max, boolean enableMaxLength) {
        StringBuilder builder = new StringBuilder();
        int loopCount = enableMaxLength ? max : randomInt(min, max);
        for (int i = 0; i < loopCount; i++)
            builder.append(NUMBER_ARRAY[randomInt(NUMBER_ARRAY.length - 1)]);
        return builder.toString();
    }

    /**
     * #brief: 生成指定最大值的随机整数
     *
     * <p>该方法生成一个从 0 到指定最大值（包括最大值）的随机整数。
     *
     * @return 生成的随机整数
     */
    public static int randomInt() {
        return randomInt(0, Integer.MAX_VALUE);
    }

    /**
     * #brief: 生成指定最大值的随机整数
     *
     * <p>该方法生成一个从 0 到指定最大值（包括最大值）的随机整数。
     *
     * @param max 随机整数的最大值（包括）
     * @return 生成的随机整数
     */
    public static int randomInt(int max) {
        return randomInt(0, max);
    }

    /**
     * #brief: 生成指定范围的随机整数
     *
     * <p>该方法生成一个从指定最小值到最大值（包括最大值）的随机整数。
     *
     * @param min 随机整数的最小值（包括）
     * @param max 随机整数的最大值（包括）
     * @return 生成的随机整数
     */
    public static int randomInt(int min, int max) {
        return (int) randomDouble(min, max);
    }

    /**
     * #brief: 生成指定最大值的随机浮点数
     *
     * <p>该方法生成一个从 0 到指定最大值（包括最大值）的随机浮点数。
     *
     * @return 生成的随机浮点数
     */
    public static float randomFloat() {
        return randomFloat(0, Float.MAX_VALUE);
    }

    /**
     * #brief: 生成指定最大值的随机浮点数
     *
     * <p>该方法生成一个从 0 到指定最大值（包括最大值）的随机浮点数。
     *
     * @param max 随机浮点数的最大值（包括）
     * @return 生成的随机浮点数
     */
    public static float randomFloat(float max) {
        return randomFloat(0, max);
    }

    /**
     * #brief: 生成指定范围的随机浮点数
     *
     * <p>该方法生成一个从指定最小值到最大值（包括最大值）的随机浮点数。
     *
     * @param min 随机浮点数的最小值（包括）
     * @param max 随机浮点数的最大值（包括）
     * @return 生成的随机浮点数
     */
    public static float randomFloat(float min, float max) {
        return (float) randomDouble(min, max);
    }

    /**
     * #brief: 生成指定最大值的随机双精度数
     *
     * <p>该方法生成一个从 0 到指定最大值（包括最大值）的随机双精度数。
     *
     * @return 生成的随机双精度数
     */
    public static double randomDouble() {
        return randomDouble(0, Double.MAX_VALUE);
    }

    /**
     * #brief: 生成指定最大值的随机双精度数
     *
     * <p>该方法生成一个从 0 到指定最大值（包括最大值）的随机双精度数。
     *
     * @param max 随机双精度数的最大值（包括）
     * @return 生成的随机双精度数
     */
    public static double randomDouble(double max) {
        return randomDouble(0, max);
    }

    /**
     * #brief: 生成指定范围的随机双精度数
     *
     * <p>该方法生成一个从指定最小值到最大值（包括最大值）的随机双精度数。
     *
     * @param min 随机双精度数的最小值（包括）
     * @param max 随机双精度数的最大值（包括）
     * @return 生成的随机双精度数
     */
    public static double randomDouble(double min, double max) {
        return (Math.random() * ((max - min) + 1.0f)) + min;
    }

    /**
     * #brief: 生成随机布尔值
     *
     * <p>该方法生成一个随机的布尔值，`true` 或 `false`，用于表示随机事件的结果。
     * 该方法利用 Java 标准库中的 `Random` 类生成布尔值。
     *
     * @return 随机生成的布尔值
     */
    public static boolean randomBoolean() {
        return new Random().nextBoolean();
    }

    /**
     * #brief: 生成 UUID 的 16 位小写 MD5 哈希值
     *
     * <p>该方法基于随机生成的 UUID，计算其 MD5 哈希值，并返回前 16 位的小写字符串。
     *
     * @return 16 位小写 MD5 哈希值
     */
    public static String b16md5() {
        return Codec.MD5.lower16(uuid());
    }

    /**
     * #brief: 生成 UUID 的 32 位小写 MD5 哈希值
     *
     * <p>该方法基于随机生成的 UUID，计算其完整的 32 位小写 MD5 哈希值。
     *
     * @return 32 位小写 MD5 哈希值
     */
    public static String b32md5() {
        return Codec.MD5.lower32(uuid());
    }

    /**
     * #brief: 生成 UUID 的 64 位小写 MD5 哈希值
     *
     * <p>该方法通过拼接两个 32 位 MD5 哈希值，生成 64 位的哈希字符串。
     *
     * @return 64 位小写 MD5 哈希值
     */
    public static String b64md5() {
        return b32md5() + b32md5();
    }

    /**
     * #brief: 生成 UUID 的 128 位小写 MD5 哈希值
     *
     * <p>该方法通过拼接两个 64 位 MD5 哈希值，生成 128 位的哈希字符串。
     *
     * @return 128 位小写 MD5 哈希值
     */
    public static String b128md5() {
        return b64md5() + b64md5();
    }

    /**
     * #brief: 生成 UUID 的 256 位小写 MD5 哈希值
     *
     * <p>该方法通过拼接两个 128 位 MD5 哈希值，生成 256 位的哈希字符串。
     *
     * @return 256 位小写 MD5 哈希值
     */
    public static String b256md5() {
        return b128md5() + b128md5();
    }

}

