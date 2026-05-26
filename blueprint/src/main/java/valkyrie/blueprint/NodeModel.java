package valkyrie.blueprint;

import lombok.Getter;
import lombok.Setter;
import valkyrie.utils.Generator;

/**
 * @author Luo Tiansheng
 * @since 2026/5/26
 */
public class NodeModel
{
        @Getter
        @Setter
        private String title;

        double x;

        double y;

        double w;

        double h;

        public NodeModel(String title, double x, double y, double w, double h)
        {
                this.title = title;
                this.x = x;
                this.y = y;
                this.w = w;
                this.h = h;
        }
}
