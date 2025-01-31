package org.domotics;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DomoticsMIB {
    private static final DomoticsMIB INSTANCE = new DomoticsMIB();

    // OID Structure:
    // 1.1 - Lighting
    //   1.1.X - Zone X status (0=off, 1=on)
    //   1.1.X.Y - Zone X intensity (Y=0-100)
    // 1.2 - Temperature
    //   1.2.X - Zone X current temp
    // 1.3 - AC
    //   1.3.X - Zone X status (0=off, 1=heat, 2=cool)
    //   1.3.X.Y - Zone X target temp

    private final Map<String, ConcurrentHashMap<String, Object>> mib = new ConcurrentHashMap<>();

    private DomoticsMIB() {
        // Initialize MIB structure
        mib.put("1.1", new ConcurrentHashMap<>()); // Lighting
        mib.put("1.2", new ConcurrentHashMap<>()); // Temperature
        mib.put("1.3", new ConcurrentHashMap<>()); // AC

        // Load persistent configuration
        loadPersistentDevices();
    }

    public static DomoticsMIB getInstance() {
        return INSTANCE;
    }

    public synchronized void updateValue(String source, String clientIP, String oid, Object newValue) {
        validateOID(oid);
        String mainGroup = getMainGroup(oid);

        if (mib.containsKey(mainGroup)) {
            Object oldValue = mib.get(mainGroup).get(oid);
            mib.get(mainGroup).put(oid, newValue);
            Logger.logChange(source, clientIP, oid, oldValue, newValue);
        } else {
            throw new IllegalArgumentException("Invalid OID group: " + mainGroup);
        }
    }

    public synchronized Object getValue(String oid) {
        validateOID(oid);
        String mainGroup = getMainGroup(oid);
        return mib.getOrDefault(mainGroup, new ConcurrentHashMap<>())
                .getOrDefault(oid, "No such instance");
    }

    public List<String> getAllOIDs() {
        List<String> oids = new ArrayList<>();
        mib.forEach((group, entries) -> oids.addAll(entries.keySet()));
        return oids;
    }

    private String getMainGroup(String oid) {
        String[] parts = oid.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid OID format: " + oid);
        }
        return parts[0] + "." + parts[1];
    }

    private void validateOID(String oid) {
        if (!oid.matches("^\\d+(\\.\\d+)*$")) {
            throw new IllegalArgumentException("Invalid OID format: " + oid);
        }
    }

    private void loadPersistentDevices() {
        try {
            // Get path to package directory
            Path configPath = Paths.get(
                    getClass().getProtectionDomain().getCodeSource().getLocation().toURI()
            ).resolve("org/domotics/domotics.config");

            // Create parent directories if needed
            Files.createDirectories(configPath.getParent());

            if (!Files.exists(configPath)) {
                createDefaultConfig(configPath);
            }

            loadConfig(configPath);

        } catch (Exception e) {
            System.err.println("Error handling config: " + e.getMessage());
            System.err.println("Using default in-memory configuration");
        }
    }

    private void createDefaultConfig(Path configPath) {
        try (BufferedWriter writer = Files.newBufferedWriter(configPath)) {
            writer.write("# Default Domotics Configuration\n");
            writer.write("1.1.1=0\n");
            writer.write("1.1.1.0=0\n");
            writer.write("1.2.1=22.0\n");
            writer.write("1.3.1=0\n");
            writer.write("1.3.1.0=22.0\n");
            System.out.println("Created default config at: " + configPath);
        } catch (IOException e) {
            System.err.println("Couldn't create default config: " + e.getMessage());
        }
    }

    private void loadConfig(Path configPath) {
        try (BufferedReader reader = Files.newBufferedReader(configPath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty()) continue;

                String[] parts = line.split("=");
                if (parts.length == 2) {
                    String oid = parts[0].trim();
                    String value = parts[1].trim();
                    updateValue("SYSTEM", "CONFIG", oid, parseValue(value));
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading config: " + e.getMessage());
        }
    }

    private Object parseValue(String value) {
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return value;
        }
    }
}