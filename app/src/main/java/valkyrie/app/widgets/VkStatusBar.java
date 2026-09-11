package valkyrie.app.widgets;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * 底部状态栏。
 * <p>
 * 分区展示：连接上下文（连接名 / catalog / schema）、数据库类型与版本、
 * 最近一次执行结果、后台任务进度、编辑器光标与选区位置。
 * <p>
 * 全局单例，各工作区在状态变化时调用对应的 setter 推送数据。
 *
 * @author Luo Tiansheng
 * @since 2026/5/18
 */
public class VkStatusBar extends HBox
{
        private static final VkStatusBar INSTANCE = new VkStatusBar();

        private final Label connection = part("vk-status-connection");
        private final Label database = part("vk-status-database");
        private final Label execution = part("vk-status-execution");
        private final Label task = part("vk-status-task");
        private final Label cursor = part("vk-status-cursor");
        private final ProgressIndicator busy = new ProgressIndicator();

        private VkStatusBar()
        {
                getStyleClass().add("vk-status-bar");
                setAlignment(Pos.CENTER_LEFT);

                busy.setPrefSize(12, 12);
                busy.setMaxSize(12, 12);
                busy.setVisible(false);
                busy.setManaged(false);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                getChildren().addAll(connection, database, execution, spacer, busy, task, cursor);

                clearContext();
                setExecution("就绪");
                clearTask();
                clearCursor();
        }

        public static VkStatusBar getInstance()
        {
                return INSTANCE;
        }

        private static Label part(String styleClass)
        {
                Label label = new Label();
                label.getStyleClass().addAll("vk-status-part", styleClass);
                label.setMaxHeight(Double.MAX_VALUE);
                label.setVisible(false);
                label.setManaged(false);
                return label;
        }

        private static void set(Label label, String text)
        {
                boolean visible = text != null && !text.isBlank();

                label.setText(visible ? text : "");
                label.setVisible(visible);
                label.setManaged(visible);
        }

        /**
         * 设置当前连接上下文
         */
        public void setConnection(String connectionName, String catalog, String schema)
        {
                StringBuilder builder = new StringBuilder();

                builder.append("连接: ").append(connectionName == null ? "未命名" : connectionName);

                if (catalog != null && !catalog.isBlank())
                        builder.append(" · ").append(catalog);

                if (schema != null && !schema.isBlank())
                        builder.append(" · ").append(schema);

                set(connection, builder.toString());
        }

        public void clearContext()
        {
                set(connection, "未连接");
                set(database, null);
        }

        /**
         * 设置数据库类型与版本
         */
        public void setDatabase(String text)
        {
                set(database, text);
        }

        /**
         * 设置最近一次执行结果
         */
        public void setExecution(String text)
        {
                set(execution, text);
        }

        /**
         * 设置后台任务进度，{@code text} 为 {@code null} 时隐藏
         */
        public void setTask(String text)
        {
                boolean running = text != null && !text.isBlank();

                set(task, running ? text : null);
                busy.setVisible(running);
                busy.setManaged(running);
        }

        public void clearTask()
        {
                setTask(null);
        }

        /**
         * 设置编辑器光标与选区位置
         */
        public void setCursor(int line, int column, int selected)
        {
                if (line <= 0) {
                        clearCursor();
                        return;
                }

                StringBuilder builder = new StringBuilder();
                builder.append("行 ").append(line).append(", 列 ").append(column);

                if (selected > 0)
                        builder.append(" · 选中 ").append(selected);

                set(cursor, builder.toString());
        }

        private void clearCursor()
        {
                set(cursor, null);
        }
}
