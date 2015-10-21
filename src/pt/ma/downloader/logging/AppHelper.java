package pt.ma.downloader.logging;

import java.io.Closeable;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;



/**
 * 
 * 
 *
 */
public final class AppHelper {
	
	/**
	 * 
	 */
    private static Logger LOGGER;

    /**
     * 
     * @return
     */
    public static String getAppPath() {
        return null;
    }

    /**
     * 
     * @param closeable
     */
    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ex) {
                // Ignore errors
            }
        }
    }

    /**
     * 
     * @param msg
     */
    public static void logInfo(String msg) {
        getLogger().log(Level.INFO, msg);
    }

    /**
     * 
     * @param msg
     */
    public static void logError(String msg) {
        getLogger().log(Level.SEVERE, msg);
    }

    /**
     * 
     * @param msg
     * @param t
     */
    public static void logError(String msg, Throwable t) {
        getLogger().log(Level.SEVERE, msg, t);
    }

    /**
     * 
     * @return
     */
    private static Logger getLogger() {
        if (LOGGER == null) {
            LOGGER = Logger.getLogger("Moises");
            try {
                String filename = getAppPath() + "LOG" + Utilities.dameDataHora() + ".HTML";
                FileHandler fileHandler = new FileHandler(filename, true);
                fileHandler.setFormatter(new MyHtmlFormatter());
                LOGGER.addHandler(fileHandler);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return LOGGER;
    }

    /**
     * 
     * @param msg
     * @param exitApp
     */
    public static void showError(String msg, boolean exitApp) {
        String text = "Ocorreu um erro grave:\n" + msg +
                (exitApp ? "\na aplicação vai encerrar!" : "") +
                "\nConsulte o registo de erros: AppErrors.log\n ";
        JOptionPane.showMessageDialog(null, text, "Erro grave!",
                JOptionPane.ERROR_MESSAGE);
        if (exitApp) {
            System.exit(0);
        }
    }

}
