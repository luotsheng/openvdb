package valkyrie.app.pane;

import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import valkyrie.app.Application;
import valkyrie.app.assets.Assets;
import valkyrie.app.widgets.VkContextMenu;
import valkyrie.app.widgets.VkTextField;

import java.util.ArrayList;
import java.util.List;

import static valkyrie.utils.string.StrStaticImports.fmt;
import static valkyrie.utils.string.StrStaticImports.lowercase;

/**
 * 执行日志面板（只读），支持关键字搜索/高亮定位。
 *
 * @author Luo Tiansheng
 * @since 2026/4/2
 */
public class ExecuteLoggerPane extends BorderPane
{
        private final CodeArea codeArea;
        private final VkTextField search = new VkTextField();
        private final Label matchLabel = new Label();
        private final PauseTransition searchDelay = new PauseTransition(Duration.millis(150));
        private final List<Integer> matches = new ArrayList<>();
        private int matchIndex = -1;

        public ExecuteLoggerPane()
        {
                codeArea = new CodeArea();
                codeArea.setEditable(false);

                setupContextMenu();

                search.setPromptText("搜索日志...");
                search.setPrefWidth(240);

                HBox searchBox = new HBox(5, Assets.use("search"), search, matchLabel);
                searchBox.setAlignment(Pos.CENTER_LEFT);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox top = new HBox(spacer, searchBox);
                top.setAlignment(Pos.CENTER_RIGHT);

                setTop(top);
                setCenter(new VirtualizedScrollPane<>(codeArea));

                setupSearch();
        }

        private void setupSearch()
        {
                searchDelay.setOnFinished(event -> updateSearch());

                search.textProperty().addListener((obs, oldVal, newVal) -> searchDelay.playFromStart());

                /* 回车跳到下一处匹配 */
                search.setOnAction(event -> {
                        if (!matches.isEmpty()) {
                                matchIndex = (matchIndex + 1) % matches.size();
                                jumpToMatch();
                        }
                });
        }

        private void updateSearch()
        {
                String text = search.getText();
                String source = codeArea.getText();

                matches.clear();
                matchIndex = -1;
                matchLabel.setText("");

                if (text == null || text.isBlank() || source.isEmpty())
                        return;

                String sourceLower = lowercase(source);
                String keyword = lowercase(text).trim();

                int from = 0;
                while (true) {
                        int index = sourceLower.indexOf(keyword, from);

                        if (index < 0)
                                break;

                        matches.add(index);
                        from = index + keyword.length();
                }

                if (!matches.isEmpty()) {
                        matchIndex = 0;
                        jumpToMatch();
                }
        }

        private void jumpToMatch()
        {
                if (matches.isEmpty())
                        return;

                int start = matches.get(matchIndex);
                int end = start + search.getText().trim().length();

                codeArea.selectRange(start, end);
                codeArea.requestFollowCaret();

                matchLabel.setText(fmt("%d/%d", matchIndex + 1, matches.size()));
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
                updateSearch();
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

                if (!matches.isEmpty())
                        updateSearch();
        }
}
