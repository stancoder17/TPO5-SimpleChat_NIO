/**
 *
 *  @author Dyrda Stanisław S31552
 *
 */

package zad1;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class ChatClientTask extends FutureTask<String> {

    private ChatClientTask(Callable<String> task) {
        super(task);
    }

    public static ChatClientTask create(ChatClient c, List<String> msgs, int wait) {
        return new ChatClientTask(() -> {
            /*SocketChannel sc = c.getChannel();
            c.login();

            if (wait != 0)
                Thread.sleep(wait);

            try (Selector selector = Selector.open()) {
                sc.register(selector, SelectionKey.OP_READ);
                sc.register(selector, SelectionKey.OP_WRITE);

                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iter = keys.iterator();

                for (String msg : msgs) {
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove();

                        if (key.isReadable()) {
                            SocketChannel channel = (SocketChannel) key.channel();
                            c.addToChatView(BufferOperations.readMessage(channel));
                            continue;
                        }

                        if (key.isWritable()) {
                            c.send(msg);
                            if (wait != 0)
                                Thread.sleep(wait);
                        }
                    }
                }
                c.logout();
                if (wait != 0)
                    Thread.sleep(wait);
            }*/

            return null;
        });
    }

    public ChatClient getClient() {
        return null;
    }
}
