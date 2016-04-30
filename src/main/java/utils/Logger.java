package utils;

import java.io.PrintStream;

public class Logger {
    public static final int ALL = Integer.MIN_VALUE;
    public static final int WARNINGS = 0xF;
    public static final int ERRORS = 0xFF;
    public static final int NOTHING = Integer.MAX_VALUE;

    private PrintStream out;
    private String info;
    private String warn;
    private String err;

    private int level;

    public Logger(PrintStream out, int level) {
        this.out = out;
        this.level = level;
        this.info = "[INFO] [%s] [%s.%s] %s";
        this.warn = "[WARNING] [%s] [%s.%s] %s";
        this.err = "[ERROR] [%s] [%s.%s] %s";
    }

    public synchronized void error(Object error) {
        if (level > ERRORS) return;
        out.println(String.format(this.err, //new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
                Thread.currentThread().getName(),
                Thread.currentThread().getStackTrace()[2].getClassName(),
                Thread.currentThread().getStackTrace()[2].getMethodName(),
                error));
    }

    public synchronized void warn(Object warn) {
        if (level > WARNINGS) return;
        out.println(String.format(this.warn, //new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
                Thread.currentThread().getName(),
                Thread.currentThread().getStackTrace()[2].getClassName(),
                Thread.currentThread().getStackTrace()[2].getMethodName(),
                warn));
    }

    public synchronized void info(Object info) {
        if (level > ALL) return;
        out.println(String.format(this.info, //new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
                Thread.currentThread().getName(),
                Thread.currentThread().getStackTrace()[2].getClassName(),
                Thread.currentThread().getStackTrace()[2].getMethodName(),
                info));
    }
}
