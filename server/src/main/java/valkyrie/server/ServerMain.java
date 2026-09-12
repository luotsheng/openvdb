package valkyrie.server;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Valkyrie 数据层进程入口。
 * <p>
 * 该进程不包含任何 UI，由 Electron 主进程以子进程方式拉起，通过标准输入输出
 * 交换按行分隔的 JSON 消息（stdin/stdout 管道，不监听任何端口）。
 * <p>
 * 标准输出专用于协议数据，日志一律写入标准错误；标准输入读到 EOF 说明父进程
 * 已经退出，此时本进程主动结束，避免留下僵尸进程。
 *
 * @author Luo Tiansheng
 * @since 2026/9/12
 */
public final class ServerMain
{
        private ServerMain()
        {
        }

        public static void main(String[] args)
        {
                /* 协议专用输出：后续任何第三方库写 System.out 都会被重定向到标准错误，
                   避免污染 JSON-RPC 协议流 */
                PrintStream out = new PrintStream(
                        new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);

                System.setOut(new PrintStream(
                        new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8));

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(System.in, StandardCharsets.UTF_8));

                RpcServer server = new RpcServer(out);
                server.ready();

                try {
                        String line;

                        while ((line = in.readLine()) != null) {
                                if (!line.isBlank())
                                        server.handle(line);
                        }
                } catch (Exception ignored) {
                        /* 管道关闭，按父进程退出处理 */
                } finally {
                        server.shutdown();
                }
        }
}
