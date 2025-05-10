package zad1;

import java.util.ArrayList;
import java.util.List;

public class Log {
    private final List<String> list = new ArrayList<>();

    public void addToChatView(String msg) {
        list.add(msg);
    }

    public String getLog() {
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            sb.append(s).append("\n");
        }
        return sb.toString();
    }
}
