package zad1;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class Protocol {
    public static final int HEADER_SIZE = Integer.BYTES; // number of bytes representing an int
    public static final int UNKNOWN = -1;
    public static final int LOGIN = 0;
    public static final int LOGOUT = 1;
    public static final int CLIENT_MESSAGE = 2;
    public static final int SERVER_MESSAGE = 3;

    public static int readHeader(SocketChannel sc) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(HEADER_SIZE);
        int totalBytesRead = 0;

        while (totalBytesRead < HEADER_SIZE) {
            int bytesRead = sc.read(buf);
            if (bytesRead == -1)
                throw new EOFException();
            else if (bytesRead == 0) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                continue;
            }
            totalBytesRead += bytesRead;

        }
        buf.flip();

        int header = buf.getInt();
        switch (header) {
            case LOGIN:
                case LOGOUT:
                    case CLIENT_MESSAGE:
                        case SERVER_MESSAGE:
                            return header;
            default:
                return UNKNOWN;
        }
    }

    public static int readLength(SocketChannel sc) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(HEADER_SIZE);
        int totalBytesRead = 0;

        while (totalBytesRead < HEADER_SIZE) {
            int bytesRead = sc.read(buf);
            if (bytesRead == -1) {
                throw new EOFException();
            }
            if (bytesRead == 0) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException();
                }
                continue;
            }
            totalBytesRead += bytesRead;
        }

        buf.flip();
        return buf.getInt();
    }

    public static String readMessage(SocketChannel sc, int length) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(length);
        int totalBytesRead = 0;

        while (totalBytesRead < length) {
            int bytesRead = sc.read(buf);
            if (bytesRead == -1) {
                throw new EOFException();
            }
            if (bytesRead == 0) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException();
                }
                continue;
            }
            totalBytesRead += bytesRead;
        }

        buf.flip();
        return StandardCharsets.UTF_8.decode(buf).toString().trim();
    }

    public static void writeMessage(SocketChannel sc, String message, int header) throws IOException {
        byte[] body = (message + "\n").getBytes(StandardCharsets.UTF_8);

        ByteBuffer buf = ByteBuffer.allocate(2 * Protocol.HEADER_SIZE + body.length); // header + length + message
        buf.putInt(header);      // header
        buf.putInt(body.length);         // length
        buf.put(body);                   // message body
        buf.flip();

        while (buf.hasRemaining()) {
            sc.write(buf);
        }
    }
}