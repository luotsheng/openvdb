package valkyrie.driver.api.exception;

import net.sf.jsqlparser.JSQLParserException;
import valkyrie.utils.exception.Causes;
import valkyrie.utils.exception.SystemRuntimeException;

import static valkyrie.utils.string.StrStaticImports.strtok;

/**
 * @author Luo Tiansheng
 * @since 2026/6/12
 */
public class ParserException extends SystemRuntimeException
{
        public ParserException()
        {
        }

        public ParserException(JSQLParserException e)
        {
                this(strtok(Causes.message(e), "\n")[0]);
        }

        public ParserException(Throwable e)
        {
                super(e);
        }

        public ParserException(String fmt, Object... args)
        {
                super(fmt, args);
        }

        public ParserException(String fmt, Throwable e, Object... args)
        {
                super(fmt, e, args);
        }
}
