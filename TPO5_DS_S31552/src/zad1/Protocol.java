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
    public static final int MESSAGE = 2;

    public static int readHeader(SocketChannel sc) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(HEADER_SIZE);
        while (buf.hasRemaining()) {
            if (sc.read(buf) == -1) throw new EOFException();
        }
        buf.flip();

        int header = buf.getInt();
        switch (header) {
            case LOGIN:
            case LOGOUT:
            case MESSAGE:
                return header;
            default:
                return UNKNOWN;
        }
    }

    public static int readLength(SocketChannel sc) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(HEADER_SIZE);
        while (buf.hasRemaining()) {
            if (sc.read(buf) == -1) throw new EOFException();
        }
        buf.flip();
        return buf.getInt();
    }

    public static String readMessage(int length, SocketChannel sc) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(length);
        StringBuilder sb = new StringBuilder();
        int bytesRead;

        while (buf.hasRemaining()) {
            bytesRead = sc.read(buf);
            if (bytesRead == -1) {
                throw new EOFException("End of stream reached, no data read.");
            }
            if (bytesRead == 0) { // No data available yet
                continue;
            }
            buf.flip();
            sb.append(StandardCharsets.UTF_8.decode(buf));
            buf.clear();
        }

        return sb.toString().trim();
    }
}
