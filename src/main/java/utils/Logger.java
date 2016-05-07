package utils;

import java.io.PrintStream;

public class Logger {

    public enum Level {INFO, WARNING, ERROR, NOTHING}

    private PrintStream out;
    private Level level;

    private static final String fmt = "[%s] [%s] %s";

    public Logger() {
        this(System.out, Level.INFO);
    }

    public Logger(PrintStream out) {
        this(out, Level.INFO);
    }

    public Logger(PrintStream out, Level level) {
        this.out = out;
        this.level = level;
    }

    public synchronized void log(String message, Level level) {
        out.println(String.format(fmt, level.toString(), Thread.currentThread().getName(), message));
    }

    public void error(String msg) {
        if (level.compareTo(Level.ERROR) > 0) return;
        log(msg, Level.ERROR);
    }

    public void warning(String msg) {
        if (level.compareTo(Level.WARNING) > 0) return;
        log(msg, Level.WARNING);
    }

    public void info(String msg) {
        if (level.compareTo(Level.INFO) > 0) return;
        log(msg, Level.INFO);
    }
}
