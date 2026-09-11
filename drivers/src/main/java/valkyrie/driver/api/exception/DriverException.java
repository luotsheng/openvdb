package valkyrie.driver.api.exception;

import lombok.Getter;
import valkyrie.utils.exception.Causes;

import java.sql.SQLException;

/**
 * 驱动运行时异常
 *
 * @author Luo Tiansheng
 * @since 2026/4/11
 */
@Getter
public class DriverException extends RuntimeException
{
        private int errorCode;

        public DriverException(Throwable cause)
        {
                super(Causes.original(cause));

                if (getCause() instanceof SQLException sql)
                        errorCode = sql.getErrorCode();
        }

        public DriverException(String message)
        {
                super(message);
        }
}
