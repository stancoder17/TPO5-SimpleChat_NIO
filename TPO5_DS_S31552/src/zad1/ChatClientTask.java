/**
 *
 *  @author Dyrda Stanisław S31552
 *
 */

package zad1;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class ChatClientTask extends FutureTask<Void> {

    private final ChatClient client;

    private ChatClientTask(Callable<Void> callable, ChatClient client) {
        super(callable);
        this.client = client;
    }

    public static ChatClientTask create(ChatClient c, List<String> msgs, int wait) {
        Callable<Void> task = () -> {
            try {
                c.login();
                if (wait > 0) Thread.sleep(wait);

                for (String msg : msgs) {
                    c.send(Protocol.CLIENT_MESSAGE, msg);
                    if (wait > 0) Thread.sleep(wait);
                }

                c.logout();
                if (wait > 0) Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore the interrupt status
            }
            return null;
        };
        return new ChatClientTask(task, c);
    }

    public ChatClient getClient() {
        return client;
    }
}

