package org.domotics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DomoticsMIB {
    // Singleton instance
    private static final DomoticsMIB INSTANCE = new DomoticsMIB();

    // Store MIB entries (IID -> Value)
    // OID Structure:
    // 1.1 - Lighting
    //   1.1.X - Zone X status (0=off, 1=on)
    //   1.1.X.Y - Zone X intensity (Y=0-100)
    // 1.2 - Temperature
    //   1.2.X - Zone X current temp
    // 1.3 - AC
    //   1.3.X - Zone X status (0=off, 1=heat, 2=cool)
    //   1.3.X.Y - Zone X target temp
    private final Map<String, Object> mib = new ConcurrentHashMap<>();
    private final VirtualDevice devices = new VirtualDevice();


    // Private constructor
    private DomoticsMIB() {
        // Initialize with default values
        // Initialize Lighting for 3 zones
        mib.put("1.1.1", 0);   // Zone 1 Light (OFF)
        mib.put("1.1.2", 1);   // Zone 2 Light (ON)
        mib.put("1.1.3", 0);   // Zone 3 Light (OFF)

        // Initialize Temperature for 3 zones
        mib.put("1.2.1", 22.5); // Zone 1 Temperature (22.5°C)
        mib.put("1.2.2", 24.0); // Zone 2 Temperature (24.0°C)
        mib.put("1.2.3", 20.0); // Zone 3 Temperature (20.0°C)

        // Initialize AC for 3 zones
        mib.put("1.3.1", "0");   // Zone 1 AC (OFF)
        mib.put("1.3.2", "1");   // Zone 2 AC (HEAT)
        mib.put("1.3.3", "2");   // Zone 3 AC (COOL)

        // Initialize AC target temperatures for 3 zones
        mib.put("1.3.1.1", 25.0); // Zone 1 Target Temp (25.0°C)
        mib.put("1.3.2.1", 28.0); // Zone 2 Target Temp (28.0°C)
        mib.put("1.3.3.1", 18.0); // Zone 3 Target Temp (18.0°C)
    }

    // Get singleton instance
    public static DomoticsMIB getInstance() {
        return INSTANCE;
    }

    // Get value for IID
    public Object get(String iid) {
        return mib.get(iid);
    }

    public void set(String iid, Object value) {
        if (!mib.containsKey(iid)) {
            throw new IllegalArgumentException("Invalid OID: " + iid);
        }
        mib.put(iid, value);

        // Propagate changes to VirtualDevice
        if (iid.startsWith("1.3") && iid.split("\\.").length == 4) { // Target temp OID
            devices.setACTargetTemp(iid, Double.parseDouble(value.toString()));
        }
    }
}