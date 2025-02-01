package org.domotics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VirtualDevice {
    private final Map<String, Integer> lightIntensities = new ConcurrentHashMap<>(); // OID -> 0 or 1
    private final Map<String, Integer> acStates = new ConcurrentHashMap<>();    // OID -> 0, 1, or 2
    private final Map<String, Double> acTargetTemps = new ConcurrentHashMap<>(); // OID -> temperature

    // Set light state (0 = OFF, 1 = ON)
    public void setLightIntensity(String oid, int intensity) {
        if (intensity < 0 || intensity > 100) {
            throw new IllegalArgumentException("Light intensity must be between 0 and 100");
        }
        lightIntensities.put(oid, intensity);
    }

    // Get light intensity
    public int getLightIntensity(String oid) {
        return lightIntensities.getOrDefault(oid, 0); // Default to 0 (OFF)
    }
    // Set AC state (0 = OFF, 1 = HEAT, 2 = COOL)
    public void setACState(String oid, int mode) {
        if (mode < 0 || mode > 2) {
            throw new IllegalArgumentException("AC mode must be 0 (OFF), 1 (HEAT), or 2 (COOL)");
        }
        acStates.put(oid, mode);
    }

    // Get AC state
    public int getACState(String oid) {
        return acStates.getOrDefault(oid, 0); // Default to OFF
    }

    // Set AC target temperature
    public void setACTargetTemp(String oid, double temperature) {
        if (temperature < 10 || temperature > 40) {
            throw new IllegalArgumentException("Temperature must be between 10°C and 40°C");
        }
        acTargetTemps.put(oid, temperature);
    }

    // Get AC target temperature
    public double getACTargetTemp(String oid) {
        return acTargetTemps.getOrDefault(oid, 25.0); // Default to 25°C
    }
}