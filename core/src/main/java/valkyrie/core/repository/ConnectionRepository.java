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
import valkyrie.utils.Captor;
import valkyrie.utils.io.UFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

                if (!meta.exists())
                        Captor.call(meta::createNewFile);

                try (FileOutputStream fos = new FileOutputStream(meta)) {
                        fos.write(encryptPassword(content).getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
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

        public static List<DiskSavedConnection> loadConnections()
        {
                UFile[] files = Users.connectionDir.listFiles();
                List<DiskSavedConnection> ret = new ArrayList<>();

                if (files == null)
                        return ret;

                for (UFile file : files) {
                        UFile meta = new UFile(file, Users.META_INF);

                        if (FileUtils.isDeepEmptyDirectory(file)) {
                                file.forceDelete();
                                continue;
                        }

                        try (FileInputStream fis = new FileInputStream(meta)) {
                                byte[] bytes = fis.readAllBytes();
                                String content = new String(bytes, StandardCharsets.UTF_8);

                                DiskSavedConnection connection =
                                        JSONUtils.toJavaObject(content, DiskSavedConnection.class);
                                connection.setPassword(decryptPassword(connection.getPassword()));

                                ret.add(connection);
                        } catch (Exception e) {
                                throw new CoreException(e);
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
