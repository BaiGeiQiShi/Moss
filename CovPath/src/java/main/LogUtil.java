package moss.covpath;

import java.util.Date;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LogUtil {
    private static Logger logger;
    private static FileHandler fileHandler;
    private static Level lvl;

    public static void configureLogger(String logFilePath, Level level) throws IOException {
        logger = Logger.getLogger(LogUtil.class.getName());
        
        fileHandler = new FileHandler(logFilePath, true);
        fileHandler.setFormatter(new CustomLogFormatter());
        
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new CustomLogFormatter());

        logger.addHandler(fileHandler);
        logger.addHandler(consoleHandler);
        logger.setLevel(level);
        lvl=level;
        LogUtil.logInfo(String.format("Logger Registered. LEVEL: %s. LogFilePath: %s", level.getName(), logFilePath));
    }

    public static void logDebug(String message){
        logger.fine(message);
        if(lvl==Level.FINE || lvl==Level.FINER){
            System.out.println(message);
        }
        fileHandler.flush();
    }
    
    public static void logTrace(String message){
        logger.finer(message);
        if(lvl==Level.FINER){
            System.out.println(message);
        }
        fileHandler.flush();
    }

    public static void logInfo(String message) {
        logger.info(message);
        if(lvl== Level.INFO || lvl==Level.FINE || lvl==Level.FINER){
            System.out.println(message);
        }
        fileHandler.flush();
    }
    
    public static void logWarning(String message) {
        logger.warning(message);
        if(lvl == Level.WARNING || lvl== Level.INFO || lvl==Level.FINE || lvl==Level.FINER){
            System.out.println(message);
        }
        fileHandler.flush();
    }

    public static void closeLogger() {
        for (Handler handler : logger.getHandlers()) {
            handler.close();
        }
    }

    // Custom formatter to display [time][LEVEL] message format
    static class CustomLogFormatter extends SimpleFormatter {
	//private static final String FORMAT = "[%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS][%2$s] %3$s %n";
        private static final String FORMAT = "%s %n";

        @Override
        public synchronized String format(java.util.logging.LogRecord record) {
            return String.format(
                    FORMAT,
                    //new Date(record.getMillis()),
                    //record.getLevel(),
                    record.getMessage()
            );
        }
    }
}


