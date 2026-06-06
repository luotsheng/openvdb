package valkyrie.app.widgets;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import valkyrie.utils.collection.Lists;

import java.util.Collection;

/**
 * @author Luo Tiansheng
 * @since 2026/4/9
 */
public class VkComboBox<T> extends ComboBox<T>
{
        public VkComboBox()
        {
                setPrefWidth(200);
        }

        public VkComboBox(ObservableList<T> items)
        {
                super(items);
        }

        public VkComboBox<T> copyComboBox()
        {
                VkComboBox<T> dst = new VkComboBox<>();

                dst.getItems().addAll(this.getItems());
                dst.getSelectionModel().select(
                        this.getSelectionModel().getSelectedIndex());

                return dst;
        }
}
