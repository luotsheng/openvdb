package valkyrie.app.pane;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import valkyrie.app.Application;
import valkyrie.app.widgets.VkContextMenu;
import valkyrie.app.widgets.VkToolButton;
import valkyrie.utils.time.DateFormatter;

import java.util.List;

/**
 * 执行日志面板（只读）。
 *
 * @author Luo Tiansheng
 * @since 2026/4/2
 */
public class ExecuteLoggerPane extends BorderPane
{
        private final CodeArea codeArea;

        /**
         * 上一次追加是否为影响行数；用于把「行数 · 耗时」合并到同一行
         */
        private boolean pendingRow = false;

        /**
         * 当前语句块起始段落，用于失败时给整块加红色背景
         */
        private int blockStartParagraph = 0;

        public ExecuteLoggerPane()
        {
                codeArea = new CodeArea();
                codeArea.setEditable(false);
                codeArea.getStyleClass().add("vk-code-area");

                setupContextMenu();

                Button clearButton = new VkToolButton("清空日志", "清空", "cross");
                clearButton.setOnAction(event -> clearAll());

                Button copyAllButton = new VkToolButton("复制全部", "复制", "export");
                copyAllButton.setOnAction(event -> Application.copyToClipboard(codeArea.getText()));

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox top = new HBox(6, clearButton, copyAllButton, spacer);
                top.setAlignment(Pos.CENTER_LEFT);

                setTop(top);
                setCenter(new VirtualizedScrollPane<>(codeArea));
        }

        private void setupContextMenu()
        {
                VkContextMenu contextMenu = new VkContextMenu();

                MenuItem copyItem = new MenuItem("复制");
                copyItem.setOnAction(event -> copySelectedText());

                MenuItem copyAllItem = new MenuItem("复制全部");
                copyAllItem.setOnAction(event -> Application.copyToClipboard(codeArea.getText()));

                MenuItem selectAllItem = new MenuItem("全选");
                selectAllItem.setOnAction(event -> codeArea.selectAll());

                MenuItem clearAllItem = new MenuItem("清空");
                clearAllItem.setOnAction(event -> clearAll());

                contextMenu.getItems().addAll(
                        copyItem,
                        copyAllItem,
                        selectAllItem,
                        new SeparatorMenuItem(),
                        clearAllItem
                );

                codeArea.setContextMenu(contextMenu);
        }

        private void copySelectedText()
        {
                Application.copyToClipboard(codeArea.getSelectedText());
        }

        private void clearAll()
        {
                codeArea.replaceText("");
        }

        public void appendExecute(String text)
        {
                appendHeader("EXECUTE", "tag-execute");
                appendLine(text, "sql");
        }

        public void appendExecuteQuery(String text)
        {
                appendHeader("QUERY", "tag-query");
                appendLine(text, "sql");
        }

        public void appendExecuteUpdate(String text)
        {
                appendHeader("UPDATE", "tag-update");
                appendLine(text, "sql");
        }

        public void appendRow(int value)
        {
                pendingRow = true;
                append("│ ", "bar");
                append("↳ " + value + " rows", "row");
        }

        public void appendCost(long cost)
        {
                if (pendingRow) {
                        append("    ·    " + cost + " ms\n\n", "cost");
                        pendingRow = false;
                } else {
                        append("│ ", "bar");
                        append("↳ " + cost + " ms\n\n", "cost");
                }
        }

        public void appendError(String message)
        {
                pendingRow = false;
                append("│ ", "bar");
                append("✗ " + message.replaceAll("\n", " ") + "\n\n", "error");
                markBlockError();
        }

        private void appendHeader(String tag, String styleClass)
        {
                blockStartParagraph = codeArea.getCurrentParagraph();
                append(DateFormatter.format("HH:mm:ss") + "   ", "cost");
                append("▸ " + tag + "\n", styleClass);
        }

        private void appendLine(String text, String styleClass)
        {
                append("│ ", "bar");
                append(text.strip() + "\n", styleClass);
        }

        /**
         * 给当前语句块的所有段落加失败样式
         */
        private void markBlockError()
        {
                int end = codeArea.getCurrentParagraph();

                for (int i = blockStartParagraph; i <= end; i++)
                        codeArea.setParagraphStyle(i, List.of("stmt-error"));
        }

        /**
         * 按样式类追加文本，并保持滚动到底部
         */
        private void append(String text, String styleClass)
        {
                codeArea.append(text, styleClass);
                codeArea.moveTo(codeArea.getLength());
                codeArea.requestFollowCaret();
        }
}
