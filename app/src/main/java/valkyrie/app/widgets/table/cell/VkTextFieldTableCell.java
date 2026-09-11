package valkyrie.app.widgets.table.cell;

import javafx.application.Platform;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.InputMethodEvent;
import javafx.util.converter.DefaultStringConverter;
import valkyrie.app.dialog.MultiLineEditorDialog;
import valkyrie.app.widgets.SearchHighlight;
import valkyrie.app.workbench.ModifyCell;

import java.util.function.Supplier;

/**
 * @author Luo Tiansheng
 * @since 2026/4/3
 */
public class VkTextFieldTableCell<S> extends TextFieldTableCell<S, String>
{
        /**
         * 文本编辑器
         */
        private TextField tf;

        /**
         * 当 TextField 检测到 ESC 被按下时，才设置为 true，
         * 否则始终为 false
         */
        private boolean cancelByEsc = false;

        private boolean isCommit = false;

        /**
         * 是否处于输入法组合中（正在输入拼音）
         */
        private boolean composing = false;

        private static final String NULL_VALUE_CLASS = "vk-null-value";

        /**
         * 开始编辑前记录旧值
         */
        private String oldValue;

        public interface ModifyListener
        {
                void modify(ModifyCell cell);
        }

        private final ModifyListener modifyListener;

        /**
         * 当前搜索关键字，用于高亮命中的文本
         */
        private final Supplier<String> keywordSupplier;

        public VkTextFieldTableCell()
        {
                this(null, null);
        }

        public VkTextFieldTableCell(ModifyListener modifyListener)
        {
                this(modifyListener, null);
        }

        public VkTextFieldTableCell(ModifyListener modifyListener, Supplier<String> keywordSupplier)
        {
                super(new DefaultStringConverter());
                this.modifyListener = modifyListener;
                this.keywordSupplier = keywordSupplier;
        }

        @Override
        @SuppressWarnings("CssDeprecatedValue")
        public void updateItem(String item, boolean empty)
        {
                super.updateItem(item, empty);

                if (modifyListener != null && isCommit) {

                        if (oldValue == null && (item != null && item.isEmpty())) {
                                item = null;
                                setItem(item);
                        }

                        int colIndex = getTableView().getColumns().indexOf(getTableColumn());
                        int rowIndex = getTableRow().getIndex();
                        modifyListener.modify(new ModifyCell(colIndex, rowIndex, oldValue, item));
                        isCommit = false;

                }

                if (empty) {
                        setText(null);
                        setGraphic(null);
                        getStyleClass().remove(NULL_VALUE_CLASS);
                        return;
                }

                if (item == null) {
                        setGraphic(null);
                        setText("(NULL)");
                        if (!getStyleClass().contains(NULL_VALUE_CLASS))
                                getStyleClass().add(NULL_VALUE_CLASS);
                        return;
                }

                if (item.isEmpty()) {
                        setGraphic(null);
                        setText("(EMPTY)");
                        if (!getStyleClass().contains(NULL_VALUE_CLASS))
                                getStyleClass().add(NULL_VALUE_CLASS);
                        return;
                }

                String keyword = keywordSupplier == null ? null : keywordSupplier.get();

                /* 固定行高下多行文本会被截断，显示时把换行替换成可见标记 */
                String display = item.replaceAll("\\R", "⏎ ");

                if (SearchHighlight.matches(display, keyword)) {
                        setText(null);
                        setGraphic(SearchHighlight.flow(display, keyword));
                } else {
                        setGraphic(null);
                        setText(display);
                }

                getStyleClass().remove(NULL_VALUE_CLASS);
        }

        @Override
        public void startEdit()
        {
                oldValue = getItem();

                if (isEmpty())
                        return;

                /* 多行文本用弹窗编辑：内联单行输入框不方便 */
                if (oldValue != null && (oldValue.contains("\n") || oldValue.contains("\r"))) {
                        String edited = MultiLineEditorDialog.showDialog(oldValue);

                        if (edited != null && !edited.equals(oldValue)) {
                                setItem(edited);

                                if (modifyListener != null) {
                                        int colIndex = getTableView().getColumns().indexOf(getTableColumn());
                                        int rowIndex = getTableRow().getIndex();
                                        modifyListener.modify(
                                                new ModifyCell(colIndex, rowIndex, oldValue, edited));
                                }
                        }

                        return;
                }

                createTextField();

                /*
                 * startEdit() 会在父类中创建一个相同的 TextField 对象，该 Field 对象
                 * 会在 UI 上将我们创建 tf 对象替换。所以会导致命名单元格有值，但编辑却为
                 * 空值的问题。
                 *
                 * 所以需要延迟在 createTextField() 之后调用。并在调用后清空 Graphic。
                 */
                super.startEdit();

                setText(null);
                setGraphic(tf);

                Platform.runLater(() -> {
                        tf.requestFocus();
                        tf.selectAll();
                });
        }

        @Override
        public void commitEdit(String newValue)
        {
                isCommit = true;
                super.commitEdit(tf.getText());
        }

        @Override
        public void cancelEdit()
        {
                /* 如果不是按下 ESC 取消编辑，则始终提交 */
                if (!cancelByEsc)
                        return;

                super.cancelEdit();
        }

        private void createTextField()
        {
                tf = new TextField(getString());

                /*
                 * 跟踪输入法组合状态：组合中（正在输入拼音）不能提交，
                 * 否则会把未上屏的拼音一起取走。
                 */
                composing = false;
                tf.addEventHandler(InputMethodEvent.INPUT_METHOD_TEXT_CHANGED, event ->
                        composing = event.getComposed() != null && !event.getComposed().isEmpty());

                tf.focusedProperty().addListener((obs, oldVal, newVal) -> {
                        if (!newVal && isEditing()) {
                                /* 同样延迟一帧，避免抢在输入法上屏之前读取拼音 */
                                Platform.runLater(() -> {
                                        if (!isEditing())
                                                return;

                                        commitEdit(tf.getText());
                                        /* 清理单元格状态 */
                                        setGraphic(null);
                                        updateItem(getItem(), false);
                                });
                        }
                });

                tf.setOnKeyPressed(event -> {
                        /*
                         * 只处理 ESC 取消；不在这里处理 ENTER 提交，否则中文输入法
                         * 选词时的回车会提前把拼音一起提交进来。提交交给 setOnAction。
                         */
                        if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                                cancelByEsc = true;
                                cancelEdit();
                                cancelByEsc = false;
                        }
                });

                tf.setOnAction(event -> {
                        /*
                         * 延迟到下一帧再提交：中文输入法选词时 Enter 的 ActionEvent
                         * 可能先于组合事件到达，立即读取会把拼音取走。
                         */
                        Platform.runLater(() -> {
                                if (isEditing() && !composing)
                                        commitEdit(tf.getText());
                        });
                });
        }

        private String getString()
        {
                return getItem() == null ? "" : getConverter().toString(getItem());
        }
}
