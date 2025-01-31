package org.domotics;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static final String LOG_FILE = "domotics.log";
    private static final DateTimeFormatter dtf =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void logChange(String source, String clientIP, String oid,
                                 Object oldValue, Object newValue) {
        String entry = String.format("[%s] %s (from %s) - %s changed from %s to %s%n",
                dtf.format(LocalDateTime.now()),
                source,
                clientIP,
                oid,
                oldValue,
                newValue
        );

        try (FileWriter writer = new FileWriter(LOG_FILE, true)) {
            writer.write(entry);
        } catch (IOException e) {
            System.err.println("Couldn't write to log file: " + e.getMessage());
        }
    }
}