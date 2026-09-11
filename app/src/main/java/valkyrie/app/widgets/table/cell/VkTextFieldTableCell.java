package valkyrie.app.widgets.table.cell;

import javafx.application.Platform;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.DefaultStringConverter;
import valkyrie.app.widgets.VkTextField;
import valkyrie.app.workbench.ModifyCell;

/**
 * @author Luo Tiansheng
 * @since 2026/4/3
 */
public class VkTextFieldTableCell<S> extends TextFieldTableCell<S, String>
{
        /**
         * 文本编辑器
         */
        private VkTextField tf;

        /**
         * 当 TextField 检测到 ESC 被按下时，才设置为 true，
         * 否则始终为 false
         */
        private boolean cancelByEsc = false;

        private boolean isCommit = false;

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

        public VkTextFieldTableCell()
        {
                this(null);
        }

        public VkTextFieldTableCell(ModifyListener modifyListener)
        {
                super(new DefaultStringConverter());
                this.modifyListener = modifyListener;
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
                        getStyleClass().remove(NULL_VALUE_CLASS);
                        return;
                }

                if (item == null) {
                        setText("(NULL)");
                        if (!getStyleClass().contains(NULL_VALUE_CLASS))
                                getStyleClass().add(NULL_VALUE_CLASS);
                        return;
                }

                if (item.isEmpty()) {
                        setText("(EMPTY)");
                        if (!getStyleClass().contains(NULL_VALUE_CLASS))
                                getStyleClass().add(NULL_VALUE_CLASS);
                        return;
                }

                setText(item);
                getStyleClass().remove(NULL_VALUE_CLASS);
        }

        @Override
        public void startEdit()
        {
                oldValue = getItem();

                if (!isEmpty()) {
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
                tf = new VkTextField(getString());

                tf.focusedProperty().addListener((obs, oldVal, newVal) -> {
                        if (!newVal && isEditing()) {
                                commitEdit(tf.getText());
                                /* 清理单元格状态 */
                                setGraphic(null);
                                updateItem(getItem(), false);
                        }
                });

                tf.setOnKeyPressed(event -> {
                        switch (event.getCode()) {
                                case ESCAPE -> {
                                        cancelByEsc = true;
                                        cancelEdit();
                                        cancelByEsc = false;
                                }
                                case ENTER -> commitEdit(tf.getText());
                        }
                });

                tf.setOnAction(event -> {
                        commitEdit(tf.getText());
                });
        }

        private String getString()
        {
                return getItem() == null ? "" : getConverter().toString(getItem());
        }
}
