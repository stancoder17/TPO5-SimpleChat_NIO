package zad1;

import java.util.List;

public class Log {
    public static String toString(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            sb.append(s).append("\n");
        }
        return sb.toString();
    }
}
