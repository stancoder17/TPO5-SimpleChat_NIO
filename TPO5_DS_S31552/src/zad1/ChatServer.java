/**
 *
 *  @author Dyrda Stanisław S31552
 *
 */

package zad1;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatServer {
    private final String host;
    private final int port;
    private ServerSocketChannel ssc;
    private Selector selector;
    private final List<String> log = new LinkedList<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final Map<SocketChannel, String> clientLogins = new HashMap<>();

    public ChatServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void startServer() {
        new Thread(() -> {
            try {
                ssc = ServerSocketChannel.open();
                ssc.configureBlocking(false);
                ssc.socket().bind(new InetSocketAddress(host, port));

                selector = Selector.open();
                ssc.register(selector, SelectionKey.OP_ACCEPT);

                isRunning.set(true);
            } catch (IOException e) {
                throw new RuntimeException("An error occurred while starting the server", e);
            }
            serviceConnections();
        }).start();
    }

    public void stopServer() {
        try {
            isRunning.set(false);
            selector.wakeup();
            ssc.close();
            selector.close();

            System.out.println("Server stopped");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // The breaking point for the while loop has been set in "if (!isRunning) break" line - it fixed the ClosedSelectorException problem
    private void serviceConnections() {
        while (true) {
            try {
                // Wait for incoming operations
                try {
                    selector.select();
                } catch (ClosedSelectorException e) { // The selector has been closed
                    break;
                }

                if (!isRunning.get()) break; // BREAK POINT

                // An operation occurred
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iter = keys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();

                    if (key.isAcceptable()) { // Client requests a connection
                        SocketChannel clientChannel = ssc.accept();
                        clientChannel.configureBlocking(false);
                        clientChannel.register(selector, SelectionKey.OP_READ);
                        continue;
                    }

                    if (key.isReadable()) { // Client sends a message
                        SocketChannel clientChannel = (SocketChannel) key.channel();
                        serviceMessage(clientChannel);
                        continue;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    private void serviceMessage(SocketChannel sc) throws IOException {
        if (!sc.isOpen() || sc.socket().isClosed()) return;

        int header = Protocol.readHeader(sc);

        // Check if client is logged in
        if (!clientLogins.containsKey(sc)) {
            if (header != Protocol.LOGIN) {
                ///  INFORM CLIENT THAT HE NEEDS TO LOG IN FIRST
                sc.close();
                sc.socket().close();
                return;
            }
            int msgLength = Protocol.readLength(sc);
            String msg = Protocol.readMessage(msgLength, sc);

            if (isValidLogin(msg)) {
                clientLogins.put(sc, msg); // Adam -> put(sc, "Adam")
                log.add(getCurrentTime() + " " + msg + " logged in");
            } else {
                /// INFORM CLIENT ABOUT INCORRECT LOGIN
                sc.close();
                sc.socket().close();
            }
            return;
        }


    }

    private void writeResponse(SocketChannel sc, String message) throws IOException {
        message += "\n";

        ByteBuffer buf = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));

        while (buf.hasRemaining())
            sc.write(buf);
    }

    private boolean isValidLogin(String msg) {
        return msg != null && !msg.isEmpty();
    }

    public String getServerLog() {
        return Log.toString(log);
    }

    public String getCurrentTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.nnn"));
    }
}
