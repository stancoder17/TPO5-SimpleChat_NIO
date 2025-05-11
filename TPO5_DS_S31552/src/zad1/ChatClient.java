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
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;

public class ChatClient {
    private final String host;
    private final int port;
    private SocketChannel sc;
    private final String id;
    private final List<String> chatView = new LinkedList<>();

    public ChatClient(String host, int port, String id) {
        this.host = host;
        this.port = port;
        this.id = id;
    }

    public void login() {
        addToChatView("=== " + id + " chat view");

        int attempts = 10;
        while (attempts-- > 0) {
            try {
                sc = SocketChannel.open();
                sc.configureBlocking(false);
                sc.connect(new InetSocketAddress(host, port));

                // To make sure that the while loop stops
                int count = 0;
                while (!sc.finishConnect() && count < 50) {
                    Thread.sleep(100);
                    count++;
                }

                send(Protocol.LOGIN, id); // Send login id
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

    public void send(int header, String msg) {
        try {
            byte[] body = (msg + "\n").getBytes(StandardCharsets.UTF_8);
            ByteBuffer buf = ByteBuffer.allocate(2 * Protocol.HEADER_SIZE + body.length);
            buf.putInt(header); // LOGIN, LOGOUT, MESSAGE etc.
            buf.putInt(body.length); // message size, so that the receiver knows how big buffer he has to allocate
            buf.put(body);

            while (buf.hasRemaining()) {
                sc.write(buf);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getChatView() {
        return Log.toString(chatView);
    }

    public void addToChatView(String msg) {
        chatView.add(msg);
    }

    public SocketChannel getChannel() {
        return sc;
    }

    public String getId() {
        return id;
    }
}
