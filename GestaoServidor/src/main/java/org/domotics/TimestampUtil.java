package org.domotics;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
public class TimestampUtil {
    // For Manager: day:month:year:hours:mins:secs:ms
    public static String getCurrentTimestamp() {
        LocalDateTime now = LocalDateTime.now();
        return String.format("%02d:%02d:%04d:%02d:%02d:%02d:%03d",
                now.getDayOfMonth(), now.getMonthValue(), now.getYear(),
                now.getHour(), now.getMinute(), now.getSecond(), now.getNano() / 1_000_000);
    }

    // For Agent: days:hours:mins:secs:ms (since boot/reset)
    public static String getUptimeTimestamp(long startTime) {
        long uptimeMillis = System.currentTimeMillis() - startTime;
        long days = uptimeMillis / (24 * 60 * 60 * 1000);
        long hours = (uptimeMillis % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
        long minutes = (uptimeMillis % (60 * 60 * 1000)) / (60 * 1000);
        long seconds = (uptimeMillis % (60 * 1000)) / 1000;
        long millis = uptimeMillis % 1000;

        return String.format("%d:%02d:%02d:%02d:%03d", days, hours, minutes, seconds, millis);
    }
}