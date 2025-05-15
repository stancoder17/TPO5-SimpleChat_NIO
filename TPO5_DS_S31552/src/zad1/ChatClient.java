/**
 *
 *  @author Dyrda Stanisław S31552
 *
 */

package zad1;


import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
                while (!sc.finishConnect() && count < 500) { // 500 * 10 = 5000 ms
                    Thread.sleep(10);
                    count++;
                }

                if ( !sc.finishConnect() ) {
                    throw new IOException("Connection timeout");
                }

                send(Protocol.LOGIN, id); // Send login id
                startListening(); //... for messages
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

    /**
     * Client only sends a request, but the Server closes Client's SocketChannel.
     */
    public void logout() {
        send(Protocol.LOGOUT, null);
    }

    public void send(int header, String msg) {
        try {
            Protocol.writeMessage(sc, msg, header);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Closing the client's SocketChannel unblocks selector.select() and
     * key.isReadable() returns true, but read() returns -1 (EOF).
     * Catch EOFException is to stop the listening thread after client's logout.
     */
    public void startListening() {
        new Thread(() -> {
            try {
                Selector selector = Selector.open();
                sc.register(selector, SelectionKey.OP_READ);
                while (sc.isOpen()) {
                    selector.select();

                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> iter = keys.iterator();

                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove();
                        if (key.isReadable()) {
                            try {
                                try {
                                    int header = Protocol.readHeader(sc);
                                    if (header != Protocol.SERVER_MESSAGE) {
                                        System.out.println(header);
                                        continue;
                                    }
                                } catch (EOFException e) { // Read the documentation above
                                    selector.close();
                                    return;
                                }
                                String message = Protocol.readMessage(
                                        sc, Protocol.readLength(sc)
                                );
                                addToChatView(id + ": " + message);
                            } catch (ClosedChannelException e) {
                                break;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public String getChatView() {
        return Log.toString(chatView);
    }

    public void addToChatView(String msg) {
        chatView.add(msg);
    }
}
