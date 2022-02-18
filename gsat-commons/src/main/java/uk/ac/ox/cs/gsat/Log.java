package uk.ac.ox.cs.gsat;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * This class holds the logger of the project.
 */
public class Log {
    public static final Logger GLOBAL = Logger.getLogger("GSAT");

    static {
        InputStream stream = Log.class.getClassLoader().getResourceAsStream("logging.properties");
        try {
            LogManager.getLogManager().readConfiguration(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
