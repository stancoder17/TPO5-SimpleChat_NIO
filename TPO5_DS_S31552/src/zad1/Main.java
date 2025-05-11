/**
 *
 *  @author Dyrda Stanisław S31552
 *
 */

package zad1;


import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class Main {

  public static void main(String[] args) throws Exception {
    /*String testFileName = System.getProperty("user.home") + "/ChatTest.txt";
    List<String> test = Files.readAllLines(Paths.get(testFileName));
    String host = test.remove(0);
    int port = Integer.valueOf(test.remove(0));
    ChatServer s = new ChatServer(host, port);
    s.startServer();

    ExecutorService es = Executors.newCachedThreadPool();
    List<ChatClientTask> ctasks = new ArrayList<>();

    for (String line : test) {
      String[] elts = line.split("\t");
      String id = elts[0];
      int wait = Integer.valueOf(elts[1]);
      List<String> msgs = new ArrayList<>();
      for (int i = 2; i < elts.length; i++) msgs.add(elts[i] + ", mówię ja, " +id);
      ChatClient c = new ChatClient(host, port, id);
      ChatClientTask ctask = ChatClientTask.create(c, msgs, wait);
      ctasks.add(ctask);
      es.execute(ctask);
    }
    ctasks.forEach( task -> {
      try {
        task.get();
      } catch (InterruptedException | ExecutionException exc) {
        System.out.println("*** " + exc);
      }
    });
    es.shutdown();
    s.stopServer();

    System.out.println("\n=== Server log ===");
    System.out.println(s.getServerLog());

    ctasks.forEach(t -> System.out.println(t.getClient().getChatView()));*/

    // Tworzenie kanałów
    SocketChannel sc = SocketChannel.open();
    ServerSocketChannel ssc = ServerSocketChannel.open();

    // Serwer nasłuchuje na porcie 9090
    ssc.bind(new InetSocketAddress("localhost", 9090));

    // Konfiguracja klienta
    sc.configureBlocking(false);  // Niezblokujący klient
    sc.connect(new InetSocketAddress("localhost", 9090));

    // Czekamy, aż połączenie klienta zostanie nawiązane
    while (!sc.finishConnect()) {
      // Możesz wykonać inne operacje, np. logowanie
    }

    // Wysyłanie wiadomości
    byte[] body = ("Hello World" + "\n").getBytes(StandardCharsets.UTF_8);
    ByteBuffer buf = ByteBuffer.allocate(8 + body.length);  // 8 = 4 (header) + 4 (length)
    buf.putInt(2);  // HEADER - przykładowy numer (LOGIN, LOGOUT, MESSAGE)
    buf.putInt(body.length);  // Długość wiadomości
    buf.put(body);  // Treść wiadomości

    // Wysyłanie danych
    buf.flip();
    while (buf.hasRemaining()) {
      sc.write(buf);
    }

    // Serwer czeka na połączenie i odczytuje dane
    SocketChannel serverChannel = ssc.accept();
    serverChannel.configureBlocking(true);  // Serwer w trybie blokującym

    // Odczyt danych od klienta
    int header = Protocol.readHeader(serverChannel);  // Odczytaj header
    System.out.println("Header: " + header);

    int length = Protocol.readLength(serverChannel);  // Odczytaj długość wiadomości
    System.out.println("Length: " + length);

    String message = Protocol.readMessage(length, serverChannel);  // Odczytaj wiadomość
    System.out.println("Message: " + message);

    // Zamknięcie kanałów
    sc.close();
    ssc.close();
    serverChannel.close();
  }
}
