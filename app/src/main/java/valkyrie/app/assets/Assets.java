package valkyrie.app.assets;

import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 资源管理
 *
 * @author Luo Tiansheng
 * @since 2026/3/27
 */
public class Assets
{
        private static final int DEFAULT_SIZE = 18;
        private static final Map<String, Image> IMAGES = new HashMap<>();

        static {
                loadImages();
        }

        public static ProgressIndicator newProgressIndicator()
        {
                ProgressIndicator progressIndicator = new ProgressIndicator();
                progressIndicator.setMaxSize(DEFAULT_SIZE, DEFAULT_SIZE);
                return progressIndicator;
        }

        public static ImageView use(String name)
        {
                String[] split = name.split("@");

                ImageView imageView = new ImageView(IMAGES.get(split[0]));

                String scale = split.length > 1 ? split[1] : "1x";

                int size = switch (scale) {
                        case "2x" -> 24;
                        case "3x" -> 32;
                        case "4x" -> 40;
                        case "5x" -> 50;
                        case "6x" -> 64;
                        default   -> DEFAULT_SIZE;
                };

                imageView.setFitWidth(size);
                imageView.setFitHeight(size);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);

                return imageView;
        }

        private static void loadImages()
        {
                IMAGES.put("chain", load("/assets/icons/chain.png"));
                IMAGES.put("connect", load("/assets/icons/connect.png"));
                IMAGES.put("database0", load("/assets/icons/database0.png"));
                IMAGES.put("database1", load("/assets/icons/database1.png"));
                IMAGES.put("query", load("/assets/icons/query.png"));
                IMAGES.put("run", load("/assets/icons/run.png"));
                IMAGES.put("sql", load("/assets/icons/sql.png"));
                IMAGES.put("table", load("/assets/icons/table.png"));
                IMAGES.put("modify", load("/assets/icons/modify.png"));
                IMAGES.put("plus", load("/assets/icons/plus.png"));
                IMAGES.put("minus", load("/assets/icons/minus.png"));
                IMAGES.put("search", load("/assets/icons/search.png"));
                IMAGES.put("stop", load("/assets/icons/stop.png"));
                IMAGES.put("beautify", load("/assets/icons/beautify.png"));
                IMAGES.put("check", load("/assets/icons/check.png"));
                IMAGES.put("cross", load("/assets/icons/cross.png"));
                IMAGES.put("reload", load("/assets/icons/reload.png"));
                IMAGES.put("save", load("/assets/icons/save.png"));
                IMAGES.put("struct0", load("/assets/icons/struct0.png"));
                IMAGES.put("struct1", load("/assets/icons/struct1.png"));
                IMAGES.put("index0", load("/assets/icons/index0.png"));
                IMAGES.put("storage", load("/assets/icons/storage.png"));
                IMAGES.put("warning", load("/assets/icons/warning.png"));
                IMAGES.put("mysql", load("/assets/icons/mysql.png"));
                IMAGES.put("dm2", load("/assets/icons/dm2.png"));
                IMAGES.put("nav0", load("/assets/icons/navigation.png"));
                IMAGES.put("home", load("/assets/icons/home.png"));
                IMAGES.put("table2", load("/assets/icons/table2.png"));
                IMAGES.put("export", load("/assets/icons/export.png"));
                IMAGES.put("redis", load("/assets/icons/redis.png"));
                IMAGES.put("code", load("/assets/icons/code.png"));
        }

        private static Image load(String path)
        {
                return new Image(Objects.requireNonNull(Assets.class
                                .getResource(path))
                                .toExternalForm());
        }
}
