package valkyrie.core.utils;

import valkyrie.core.Users;
import valkyrie.core.exception.CoreException;
import valkyrie.utils.io.UFile;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;

/**
 * 本地敏感信息加解密（AES-GCM）。
 * <p>
 * 随机生成 256 位密钥并保存在用户目录（{@code ~/.valkyries/.secret}，POSIX 权限 600），
 * 用于加密连接密码等本地存储的敏感字段。为兼容历史数据，未带前缀的旧明文会原样返回，
 * 下一次保存时自动加密。
 *
 * @author Luo Tiansheng
 * @since 2026/9/11
 */
public final class SecretCipher
{
        private static final String PREFIX = "enc:v1:";
        private static final String ALGORITHM = "AES/GCM/NoPadding";
        private static final String KEY_FILE = ".secret";
        private static final int KEY_BYTES = 32;
        private static final int IV_BYTES = 12;
        private static final int TAG_BITS = 128;

        private static final Object LOCK = new Object();

        private static SecretKeySpec key;

        private SecretCipher()
        {
        }

        public static boolean isEncrypted(String value)
        {
                return value != null && value.startsWith(PREFIX);
        }

        public static String encrypt(String plain)
        {
                if (plain == null || plain.isEmpty())
                        return plain;

                try {
                        byte[] iv = new byte[IV_BYTES];
                        new SecureRandom().nextBytes(iv);

                        Cipher cipher = Cipher.getInstance(ALGORITHM);
                        cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));

                        byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

                        byte[] combined = new byte[iv.length + encrypted.length];
                        System.arraycopy(iv, 0, combined, 0, iv.length);
                        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

                        return PREFIX + Base64.getEncoder().encodeToString(combined);
                } catch (Exception e) {
                        throw new CoreException("加密连接密码失败", e);
                }
        }

        /**
         * 解密；对历史明文数据原样返回，实现平滑迁移
         */
        public static String decrypt(String value)
        {
                if (!isEncrypted(value))
                        return value;

                try {
                        byte[] combined = Base64.getDecoder().decode(value.substring(PREFIX.length()));

                        byte[] iv = Arrays.copyOfRange(combined, 0, IV_BYTES);
                        byte[] encrypted = Arrays.copyOfRange(combined, IV_BYTES, combined.length);

                        Cipher cipher = Cipher.getInstance(ALGORITHM);
                        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));

                        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
                } catch (Exception e) {
                        throw new CoreException("解密连接密码失败", e);
                }
        }

        private static SecretKeySpec key()
        {
                synchronized (LOCK) {
                        if (key == null)
                                key = loadOrCreateKey();

                        return key;
                }
        }

        private static SecretKeySpec loadOrCreateKey()
        {
                UFile file = new UFile(Users.baseDir, KEY_FILE);

                try {
                        if (file.exists())
                                return new SecretKeySpec(Base64.getDecoder().decode(file.strread().trim()), "AES");

                        UFile parent = file.getParentFile();
                        if (parent != null && !parent.exists())
                                parent.mkdirs();

                        byte[] bytes = new byte[KEY_BYTES];
                        new SecureRandom().nextBytes(bytes);

                        Files.write(file.toPath(),
                                Base64.getEncoder().encodeToString(bytes).getBytes(StandardCharsets.UTF_8));
                        restrictPermissions(file.toPath());

                        return new SecretKeySpec(bytes, "AES");
                } catch (Exception e) {
                        throw new CoreException("初始化本地密钥失败", e);
                }
        }

        private static void restrictPermissions(Path path)
        {
                try {
                        Set<PosixFilePermission> permissions = EnumSet.of(
                                PosixFilePermission.OWNER_READ,
                                PosixFilePermission.OWNER_WRITE);
                        Files.setPosixFilePermissions(path, permissions);
                } catch (Exception ignored) {
                        /* 非 POSIX 文件系统（如 Windows）忽略 */
                }
        }
}
