package valkyrie.app.widgets;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/**
 * 搜索命中高亮。
 * <p>
 * 把文本按关键字切段，命中部分用黄色底、深色字的 {@link Label} 标记，
 * 用于表格/列表搜索时标出匹配到的文本。
 *
 * @author Luo Tiansheng
 * @since 2026/9/12
 */
public final class SearchHighlight
{
        private SearchHighlight()
        {
        }

        public static boolean matches(String text, String keyword)
        {
                return text != null
                        && keyword != null
                        && !keyword.isBlank()
                        && text.toLowerCase().contains(keyword.toLowerCase());
        }

        /**
         * 普通文本（无关键字时使用）
         */
        public static Label plain(String text)
        {
                Label label = new Label(text);
                label.getStyleClass().add("search-seg");
                return label;
        }

        /**
         * 命中关键字时返回带黄色标记的文本节点，否则返回纯文本节点
         */
        public static javafx.scene.Node flow(String text, String keyword)
        {
                if (keyword == null || keyword.isBlank() || !matches(text, keyword))
                        return plain(text);

                HBox box = new HBox();
                box.setAlignment(Pos.CENTER_LEFT);

                String lowerText = text.toLowerCase();
                String lowerKeyword = keyword.toLowerCase();

                int from = 0;
                int index;

                while ((index = lowerText.indexOf(lowerKeyword, from)) >= 0) {
                        if (index > from)
                                box.getChildren().add(segment(text.substring(from, index), false));

                        box.getChildren().add(segment(
                                text.substring(index, index + lowerKeyword.length()), true));

                        from = index + lowerKeyword.length();
                }

                if (from < text.length())
                        box.getChildren().add(segment(text.substring(from), false));

                return box;
        }

        private static Label segment(String text, boolean hit)
        {
                Label label = new Label(text);
                label.getStyleClass().add("search-seg");

                if (hit)
                        label.getStyleClass().add("search-hit");

                return label;
        }
}

