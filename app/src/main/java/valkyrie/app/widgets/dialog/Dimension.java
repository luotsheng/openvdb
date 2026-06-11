package valkyrie.app.widgets.dialog;

import static valkyrie.utils.string.StrStaticImports.*;

/**
 * 提示框常量定义
 *
 * @author Luo Tiansheng
 * @since 2026/4/9
 */
class Dimension
{
        /**
         * 默认宽度
         */
        private static final int DEFAULT_WINDOW_WIDTH = 400;

        /**
         * 默认高度
         */
        private static final int DEFAULT_WINDOW_HEIGHT = 200;

        /**
         * 每行设置像素高度
         */
        private static final int SCALE_LINE_HEIGHT = 25;

        /**
         * 设置最长像素宽度
         */
        private static final int SCALE_LINE_WIDTH = 10;

        /**
         * 仅包含英文的文本
         */
        private static final int SCALE_ASCII_LINE_WIDTH = 8;

        /**
         * 宽度
         */
        final int width;

        /**
         * 高度
         */

        final int height;

        /**
         * 计算窗口合适宽高
         */
        public Dimension(String text)
        {
                int wScale = SCALE_LINE_WIDTH;

                if (strascii(text))
                        wScale = SCALE_ASCII_LINE_WIDTH;

                width = Math.max(strwidth(text) * wScale, DEFAULT_WINDOW_WIDTH);
                height = Math.max(strlines(text) * SCALE_LINE_HEIGHT, DEFAULT_WINDOW_HEIGHT);
        }
}
