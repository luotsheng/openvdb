package valkyrie.app.pane;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import valkyrie.app.Application;
import valkyrie.app.widgets.VkContextMenu;

/**
 * @author Luo Tiansheng
 * @since 2026/4/2
 */
public class ExecuteLoggerPane extends VirtualizedScrollPane<CodeArea>
{
        private final CodeArea codeArea;

        public ExecuteLoggerPane()
        {
                super(new CodeArea());

                codeArea = getContent();
                codeArea.setEditable(false);

                setupContextMenu();
        }

        private void setupContextMenu()
        {
                VkContextMenu contextMenu = new VkContextMenu();

                MenuItem copyAllItem = new MenuItem("复制");
                copyAllItem.setOnAction(event -> copySelectedText());
                MenuItem clearAllItem = new MenuItem("清空");
                clearAllItem.setOnAction(event -> clearAll());

                contextMenu.getItems().addAll(
                        copyAllItem,
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
                appendText("> Execute");
                appendText(text);
        }

        public void appendExecuteQuery(String text)
        {
                appendText("> Query");
                appendText(text);
        }

        public void appendExecuteUpdate(String text)
        {
                appendText("> Update");
                appendText(text);
        }

        public void appendRow(int value)
        {
                appendText("Row: " + value);
        }

        public void appendCost(long cost)
        {
                appendText("Time: " + cost + "ms");
        }

        public void appendError(String message)
        {
                appendText("Error: " + message.replaceAll("\n", ""));
        }

        private void appendText(String text)
        {
                text = text.strip();
                codeArea.appendText(text + "\n");
                codeArea.moveTo(codeArea.getLength());
                codeArea.requestFollowCaret();
        }
}
