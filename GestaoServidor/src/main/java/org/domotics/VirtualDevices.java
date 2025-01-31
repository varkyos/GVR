package org.domotics;

import java.time.LocalDateTime;
import java.util.Random;

public class VirtualDevices {
    // Lighting Controller
    public static class Lighting {
        public static void setPower(String zone, boolean status) {
            DomoticsMIB.getInstance().updateValue(
                    "LIGHTING",
                    "1.1." + zone,
                    status ? 1 : 0
            );
        }

        public static void setIntensity(String zone, int level) {
            if(level < 0 || level > 100) {
                throw new IllegalArgumentException("Invalid intensity");
            }
            DomoticsMIB.getInstance().updateValue(
                    "LIGHTING",
                    "1.1." + zone + ".0",
                    level
            );
        }
    }

    public static class AirConditioning {
        public static void setMode(String zone, String mode) {
            int value = switch(mode.toLowerCase()) {
                case "off" -> 0;
                case "heat" -> 1;
                case "cool" -> 2;
                default -> throw new IllegalArgumentException("Invalid mode");
            };

            DomoticsMIB.getInstance().updateValue(
                    "AC",
                    "1.3." + zone,
                    value
            );
        }

        public static void setTargetTemp(String zone, double temp) {
            DomoticsMIB.getInstance().updateValue(
                    "AC",
                    "1.3." + zone + ".0",
                    temp
            );
        }
    }

    // Environment Sensor
    public static class Environment {
        private static volatile boolean running = true;
        private static final double MIN_TEMP = 15.0;  // 15°C (winter night)
        private static final double MAX_TEMP = 30.0;  // 30°C (summer day)
        private static final double MAX_CHANGE = 0.5; // Max °C change per 5s

        public static void startMonitoring() {
            new Thread(() -> {
                Random rand = new Random();
                DomoticsMIB mib = DomoticsMIB.getInstance();

                // Initialize with realistic values
                initializeSensors(mib);

                while(running) {
                    try {
                        // Gradually change all temperature sensors
                        mib.getAllOIDs().stream()
                                .filter(oid -> oid.startsWith("1.2."))
                                .forEach(oid -> {
                                    double current = ((Number) mib.getValue(oid)).doubleValue();

                                    // Realistic change: ±0.5°C max per update
                                    double change = (rand.nextDouble() - 0.5) * MAX_CHANGE;
                                    double newTemp = current + change;

                                    // Keep within realistic bounds
                                    newTemp = Math.min(MAX_TEMP, Math.max(MIN_TEMP, newTemp));
                                    newTemp = Math.round(newTemp * 10) / 10.0; // 1 decimal place

                                    Object old = mib.getValue(oid);
                                    mib.updateValue("ENVIRONMENT", oid, newTemp);
                                    Logger.logChange("ENV_SENSOR", oid, old, newTemp);
                                });

                        Thread.sleep(5000); // Update every 5 seconds
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }).start();
        }

        private static void initializeSensors(DomoticsMIB mib) {
            Random rand = new Random();
            mib.getAllOIDs().stream()
                    .filter(oid -> oid.startsWith("1.2."))
                    .forEach(oid -> {
                        double initial = 18.0 + (7.0 * rand.nextDouble());
                        mib.updateValue("ENVIRONMENT", oid, initial);
                    });
        }

        public static void stopMonitoring() {
            running = false;
        }
    }
}