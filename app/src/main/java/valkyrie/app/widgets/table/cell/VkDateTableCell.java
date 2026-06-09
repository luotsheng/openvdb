package valkyrie.app.widgets.table.cell;

import javafx.scene.control.TableCell;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Luo Tiansheng
 * @since 2026/3/27
 */
public class VkDateTableCell<S> extends TableCell<S, Date>
{
        private final SimpleDateFormat sdf =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        @Override
        protected void updateItem(Date item, boolean empty)
        {
                super.updateItem(item, empty);

                if (empty || item == null) {
                        setText(null);
                        return;
                }

                setText(sdf.format(item));
        }
}
