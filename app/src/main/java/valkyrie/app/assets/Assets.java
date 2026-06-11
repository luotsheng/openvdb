package valkyrie.app.assets;

import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import valkyrie.app.Application;
import valkyrie.utils.Captor;
import valkyrie.utils.io.UFile;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static valkyrie.utils.string.StrStaticImports.strrstr;

/**
 * 资源管理
 *
 * @author Luo Tiansheng
 * @since 2026/3/27
 */
public class Assets
{
        private static final double DEFAULT_SIZE = 19;
        private static final Map<String, Image> originImages = new HashMap<>();

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

                ImageView imageView = new ImageView();
                String scale = split.length > 1 ? split[1] : "1x";

                double size = parseScale(scale);

                Image image = originImages.get(split[0]);
                Image scaledImage = new Image(image.getUrl(), size, size, true, true);
                imageView.setImage(scaledImage);

                return imageView;
        }

        private static double parseScale(String scale)
        {
                double size;

                if (scale.endsWith("px")) {
                        size = Double.parseDouble(scale.substring(0, scale.length() - 2));
                } else {
                        size = switch (scale) {
                                case "2x" -> 24.0f;
                                case "3x" -> 32.0f;
                                case "4x" -> 40.0f;
                                case "5x" -> 50.0f;
                                case "6x" -> 64.0f;
                                default   -> DEFAULT_SIZE;
                        };
                }

                return size;
        }

        private static void loadImages()
        {
                Captor.call(() -> {
                        URI uri = Application.getResourceURI("assets/icons");

                        UFile iconsDir = new UFile(uri);
                        UFile[] files = iconsDir.listFiles();

                        if (files != null) {
                                for (UFile icon : files)
                                        originImages.put(icon.getCleanName(), load(icon));
                        }
                });
        }

        private static String toResourcePath(UFile file)
        {
                String path = file.getPath();
                return path.substring(strrstr(path, "/", 3));
        }

        private static Image load(UFile file)
        {
                return new Image(Objects.requireNonNull(Assets.class
                                .getResource(toResourcePath(file)))
                                .toExternalForm());
        }
}
