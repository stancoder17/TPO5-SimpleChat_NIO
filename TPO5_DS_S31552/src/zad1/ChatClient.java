/**
 *
 *  @author Dyrda Stanisław S31552
 *
 */

package zad1;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class ChatClient {
    private final String host;
    private final int port;
    private SocketChannel sc;
    private final String id;
    private final Log chatView = new Log();

    public ChatClient(String host, int port, String id) {
        this.host = host;
        this.port = port;
        this.id = id;
    }

    public void login() {
        chatView.addToChatView("=== " + id + " chat view");

        int attempts = 10;
        while (attempts-- > 0) {
            try {
                sc = SocketChannel.open();
                sc.configureBlocking(false);
                sc.connect(new InetSocketAddress(host, port));

                int count = 0; // to make sure that the while loop stops
                while (!sc.finishConnect() && count < 50) { // 5 seconds in this case
                    Thread.sleep(100);
                    count++;
                }

                return;
            } catch (IOException | InterruptedException e) {
                if (attempts == 0) {
                    throw new RuntimeException(e);
                }
                try { // If server not yet ready
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    public void logout() {
        try {
            sc.close();
            sc.socket().close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void send(String req) {
        try {
            // Writing
            ByteBuffer buf = ByteBuffer.wrap((req + "\n").getBytes(StandardCharsets.UTF_8));

            while (buf.hasRemaining()) {
                sc.write(buf);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public SocketChannel getChannel() {
        return sc;
    }

    public String getChatView() {
        return chatView.getLog();
    }

    public void addToChatView(String msg) {
        chatView.addToChatView(msg);
    }

    public String getId() {
        return id;
    }
}
