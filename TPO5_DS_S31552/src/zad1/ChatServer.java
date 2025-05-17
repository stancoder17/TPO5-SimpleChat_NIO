/**
 * @author Dyrda Stanisław S31552
 */

package zad1;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ChatServer {
    private final String host;
    private final int port;
    private final List<String> log = new LinkedList<>();
    private volatile boolean isRunning = false;
    private final Map<SocketChannel, String> clientLogins = new HashMap<>();
    private ServerSocketChannel ssc;
    private Selector selector;

    public ChatServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static String getCurrentTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }

    public void startServer() {
        System.out.println("Server started");
        new Thread(() -> {
            try {
                ssc = ServerSocketChannel.open();
                ssc.configureBlocking(false);
                ssc.socket().bind(new InetSocketAddress(host, port));

                selector = Selector.open();
                ssc.register(selector, SelectionKey.OP_ACCEPT);

                isRunning = true;
            } catch (IOException e) {
                throw new RuntimeException("An error occurred while starting the server", e);
            }
            serviceConnections();
        }).start();
    }

    public void stopServer() {
        isRunning = false;
        selector.wakeup();
        System.out.println("Server stopped");
    }

    /**
     * <p>When {@code stopServer()} is called, it closes the {@code selector}.
     * This causes any blocking {@code selector.select()} call in {@code serviceConnections()}
     * to immediately throw a {@link java.nio.channels.ClosedSelectorException}.
     *
     * <p>This exception is caught inside the loop to break out cleanly,
     * ensuring the server stops without additional flags or checks.
     */

    private void serviceConnections() {
        try {
            while (isRunning) {
                selector.select(); // Block until an event or wakeup() in stopServer()

                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iter = keys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();

                    if (key.isAcceptable()) {
                        SocketChannel clientChannel = ssc.accept();
                        clientChannel.configureBlocking(false);
                        clientChannel.register(selector, SelectionKey.OP_READ);
                    } else if (key.isReadable()) {
                        SocketChannel clientChannel = (SocketChannel) key.channel();
                        serviceMessage(clientChannel);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace(); // Handle errors (unexpected)
        } finally { // Server stopping
            try {
                selector.close();
                ssc.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void serviceMessage(SocketChannel sc) throws IOException {
        /**
         * This check prevents attempts to read from a channel that has already been closed by the client.
         * In THIS program, it most commonly occurs when the client sends a LOGOUT message and immediately
         * closes its channel and socket. By the time the server attempts to read from the channel, it may
         * already be closed, leading to a ClosedChannelException or IOException. This guard avoids that.
         */
        if (!sc.isOpen() || sc.socket().isClosed()) return;

        int header = Protocol.readHeader(sc);
        String login = clientLogins.get(sc);

        if (login == null) {
            handleLogin(sc, header);
        } else {
            switch (header) {
                case Protocol.CLIENT_MESSAGE:
                    handleClientMessage(sc, login);
                    break;
                case Protocol.LOGOUT:
                    handleLogout(sc, login);
                    break;
                case Protocol.LOGIN:
                    handleAlreadyLoggedIn(sc);
                    break;
                default:
                    handleUnknown(sc);
                    break;
            }
        }
    }

    private void handleLogin(SocketChannel sc, int header) throws IOException {
        if (header != Protocol.LOGIN) {
            send(sc, "You are not logged in");
            sc.close();
            sc.socket().close();
            return;
        }

        int msgLength = Protocol.readLength(sc);
        String login = Protocol.readMessage(sc, msgLength);

        synchronized (clientLogins) {
            if (isValidLogin(login)) {
                clientLogins.put(sc, login);
                log.add(getCurrentTime() + " " + login + " logged in");
                broadcastMessage(login + " logged in");
            } else {
                send(sc, "Invalid login, ending the connection");
                sc.close();
                sc.socket().close();
            }
        }
    }

    private void handleAlreadyLoggedIn(SocketChannel sc) throws IOException {
        send(sc, "You are already logged in");
    }

    private void handleLogout(SocketChannel sc, String login) throws IOException {
        synchronized (clientLogins) {
            clientLogins.remove(sc);
        }
        sc.close();
        sc.socket().close();
        broadcastMessage(login + " logged out");
    }

    private void handleClientMessage(SocketChannel sc, String login) throws IOException {
        int msgLength = Protocol.readLength(sc);
        String message = Protocol.readMessage(sc, msgLength);
        broadcastMessage(login + ": " + message);
        log.add(getCurrentTime() + " " + login + ": " + message);
    }

    private void handleUnknown(SocketChannel sc) throws IOException {
        send(sc, "Unknown message type");
    }

    private void broadcastMessage(String message) {
        Set<SocketChannel> channelsToRemove = new HashSet<>();

        for (SocketChannel sc : clientLogins.keySet()) {
            try {
                if (!sc.isOpen() || sc.socket().isClosed()) { // If the client was disconnected during this method, we will remove him later instead of throwing an exception that will stop the server
                    channelsToRemove.add(sc);
                    continue;
                }
                send(sc, message);
            } catch (IOException e) {
                channelsToRemove.add(sc);
            }
        }

        synchronized (clientLogins) {
            for (SocketChannel sc : channelsToRemove) {
                clientLogins.remove(sc);
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
}
