package valkyrie.app.widgets.dialog;

import javafx.stage.Modality;
import javafx.stage.Stage;
import valkyrie.app.Application;

/**
 * @author Luo Tiansheng
 * @since 2026/6/7
 */
public class VkDialogStages extends Stage
{
        public static Stage create()
        {
                Stage primaryStage = Application.createModalStage();
                primaryStage.initModality(Modality.APPLICATION_MODAL);
                primaryStage.setResizable(false);
                return primaryStage;
        }
}
