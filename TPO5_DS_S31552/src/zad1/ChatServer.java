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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatServer implements Runnable {
    private final String host;
    private final int port;
    private ServerSocketChannel ssc;
    private Selector selector;
    private final Log log = new Log();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final Map<SocketChannel, String> clientLogins = new HashMap<>();

    public ChatServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void run() {
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
    }

    public void startServer() {
        Thread t = new Thread(this);
        t.start();
        System.out.println("Server started");
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

                if (!isRunning.get()) break;

                // An operation occured
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

                    if (key.isReadable()) { // Client sends a request
                        SocketChannel clientChannel = (SocketChannel) key.channel();
                        serviceRequest(clientChannel);
                        continue;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    private void serviceRequest(SocketChannel sc) throws IOException {
        if (!sc.isOpen() || sc.socket().isClosed()) return;

        String request = BufferOperations.readMessage(sc);
        String clientId = clientLogins.get(sc);

        // If client not logged in
        synchronized (clientLogins) {
            if (!clientLogins.containsKey(sc)) {
                if (isValidLogin(request)) {
                    clientId = request.split(" ")[1];
                    clientLogins.put(sc, clientId); // login Adam -> put(sc, "Adam")

                    log.addToChatView(clientId + " logged in at " + getCurrentTime());

                    writeResponse(sc, "logged in");
                } else {
                    writeResponse(sc, "incorrect login, connect again");
                    sc.close();
                    sc.socket().close();
                }
                return;
            }
        }
        String[] tokens = request.split(" ");

    }

    private void writeResponse(SocketChannel sc, String message) throws IOException {
        message += "\n";

        ByteBuffer buf = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));

        while (buf.hasRemaining())
            sc.write(buf);
    }

    private boolean isValidLogin(String msg) {
        if (msg != null) {
            String[] tokens = msg.split(" ");
            return tokens.length == 2 && tokens[0].equals("login");
        }
        return false;
    }

    public String getServerLog() {
        return log.getLog();
    }

    public String getCurrentTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.nnn"));
    }
}
