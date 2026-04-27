package server;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ServerLogger {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ServerLogger() {
    }

    /**
     * Ecrit un log standard horodate sur la sortie console.
     */
    public static synchronized void info(String message) {
        System.out.println(prefix("INFO") + message);
    }

    /**
     * Ecrit un log d'avertissement horodate sur la sortie d'erreur.
     */
    public static synchronized void warn(String message) {
        System.err.println(prefix("WARN") + message);
    }

    /**
     * Ecrit un log d'erreur horodate sur la sortie d'erreur.
     */
    public static synchronized void error(String message, Throwable throwable) {
        System.err.println(prefix("ERROR") + message);
        if (throwable != null) {
            throwable.printStackTrace(System.err);
        }
    }

    private static String prefix(String level) {
        return "[" + FORMATTER.format(LocalDateTime.now()) + "] [" + level + "] ";
    }
}
