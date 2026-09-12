package valkyrie.core.repository;

import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import valkyrie.core.Users;
import valkyrie.core.exception.CoreException;
import valkyrie.core.model.DiskSavedConnection;
import valkyrie.core.utils.FileUtils;
import valkyrie.core.utils.JSONUtils;
import valkyrie.core.utils.SecretCipher;
import valkyrie.utils.io.UFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.Collator;
import java.util.*;

/**
 * 连接信息
 *
 * @author Luo Tiansheng
 * @since 2026/3/25
 */
public class ConnectionRepository
{
        private static final Logger LOG = LoggerFactory.getLogger(ConnectionRepository.class);

        @SuppressWarnings("ResultOfMethodCallIgnored")
        public static void saveConnection(String name, String content)
        {
                UFile dir = new UFile(Users.connectionDir, name);
                UFile meta = new UFile(dir, Users.META_INF);

                if (meta.exists())
                        throw new CoreException(name + "已存在！");

                dir.mkdirs();

                /* 先写临时文件再移动，避免写入中断时留下损坏的配置 */
                UFile tmp = new UFile(dir, Users.META_INF + ".tmp");

                try {
                        Files.write(tmp.toPath(),
                                encryptPassword(content).getBytes(StandardCharsets.UTF_8));
                        Files.move(tmp.toPath(), meta.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                        tmp.forceDelete();
                        /* 删除文件夹 */
                        dir.forceDelete();
                        throw new CoreException(e);
                }
        }

        @SuppressWarnings("ResultOfMethodCallIgnored")
        public static void updateConnection(String oldName, String newName, String content)
        {
                File newDir = new File(Users.connectionDir, newName);

                if (!Objects.equals(oldName, newName)) {
                        File oldDir = new File(Users.connectionDir, oldName);
                        oldDir.renameTo(newDir);
                }

                UFile meta = new UFile(newDir, Users.META_INF);
                meta.forceDelete();

                saveConnection(newName, content);
        }

        public static void deleteConnection(String name)
        {
                new UFile(Users.connectionDir, name).forceDelete();
        }

        public static boolean exists(String name)
        {
                return name != null && new UFile(Users.connectionDir, name).exists();
        }

        public static List<DiskSavedConnection> loadConnections()
        {
                UFile[] files = Users.connectionDir.listFiles();
                List<DiskSavedConnection> ret = new ArrayList<>();

                if (files == null)
                        return ret;

                for (UFile file : files) {
                        UFile meta = new UFile(file, Users.META_INF);

                        if (!meta.isFile()) {
                                if (FileUtils.isDeepEmptyDirectory(file)) {
                                        file.forceDelete();
                                } else {
                                        LOG.warn("连接配置缺失，已跳过：{}", file.getAbsolutePath());
                                }

                                continue;
                        }

                        try (FileInputStream fis = new FileInputStream(meta)) {
                                byte[] bytes = fis.readAllBytes();
                                String content = new String(bytes, StandardCharsets.UTF_8);

                                DiskSavedConnection connection =
                                        JSONUtils.toJavaObject(content, DiskSavedConnection.class);

                                if (connection == null)
                                        throw new CoreException("连接配置内容为空或格式不正确");

                                if (connection.getName() == null || connection.getName().isBlank())
                                        connection.setName(file.getName());

                                connection.setPassword(decryptPassword(connection.getPassword()));

                                ret.add(connection);
                        } catch (Exception e) {
                                /* 单个连接配置损坏时跳过，避免整个应用无法启动 */
                                LOG.error("加载连接配置失败，已跳过：{}", meta.getAbsolutePath(), e);
                        }
                }

                Collator collator = Collator.getInstance(Locale.CHINA);
                ret.sort(Comparator.comparing(DiskSavedConnection::getName, collator));

                return ret;
        }

        /**
         * 将 JSON 中的 password 字段加密后再落盘（已是密文则保持不变）
         */
        private static String encryptPassword(String content)
        {
                if (content == null || content.isEmpty())
                        return content;

                try {
                        JSONObject json = JSONObject.parseObject(content);
                        String password = json.getString("password");

                        if (password != null && !password.isEmpty() && !SecretCipher.isEncrypted(password))
                                json.put("password", SecretCipher.encrypt(password));

                        return json.toJSONString();
                } catch (Exception e) {
                        LOG.warn("加密连接密码失败，将按原样保存", e);
                        return content;
                }
        }

        private static String decryptPassword(String password)
        {
                try {
                        return SecretCipher.decrypt(password);
                } catch (Exception e) {
                        LOG.error("解密连接密码失败，需要重新输入密码", e);
                        return null;
                }
        }

}
