/**
 *
 *  @author Dyrda Stanisław S31552
 *
 */

package zad1;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
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
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void serviceMessage(SocketChannel sc) throws IOException {
        if (!sc.isOpen() || sc.socket().isClosed()) return;

        int header = Protocol.readHeader(sc);
        String login = clientLogins.get(sc);

        // Check if client is logged in
        if (login == null) {
            if (header != Protocol.LOGIN) {
                send(sc, "You are not logged in");
                sc.close();
                sc.socket().close();
                return;
            }
            int msgLength = Protocol.readLength(sc);
            login = Protocol.readMessage(sc, msgLength);

            if (isValidLogin(login)) {
                clientLogins.put(sc, login); // Adam -> put(sc, "Adam")
                log.add(getCurrentTime() + " " + login + " logged in");
                broadcastMessage(login + " logged in");
            } else {
                send(sc, "Invalid login, ending the connection");
                sc.close();
                sc.socket().close();
            }
        }
        else if (header == Protocol.CLIENT_MESSAGE) {
            int msgLength = Protocol.readLength(sc);
            String message = Protocol.readMessage(sc, msgLength);

            broadcastMessage(login + ": " + message);
        }
        else if (header == Protocol.LOGOUT) {
            clientLogins.remove(sc);
            sc.close();
            sc.socket().close();
            broadcastMessage(login + " logged out");
        }
        else if (header == Protocol.LOGIN) { // Client is already logged in here, we check login in the first 'if' statement
            send(sc, "Already logged in");
        }
        else {
            send(sc, "Unknown message type");
        }
    }

    private void broadcastMessage(String message) {
        for (SocketChannel sc : clientLogins.keySet()) {
            try {
                send(sc, message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private void send(SocketChannel sc, String message) throws IOException {
        Protocol.writeMessage(sc, message, Protocol.SERVER_MESSAGE);
    }

    private boolean isValidLogin(String msg) {
        return msg != null && !msg.isEmpty();
    }

    public String getServerLog() {
        return Log.toString(log);
    }

    public static String getCurrentTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.nnn"));
    }
}
