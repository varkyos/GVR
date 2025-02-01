package org.domotics;

public class SNMPRequestHandler {
    private final DomoticsMIB mib = DomoticsMIB.getInstance();
    private final VirtualDevice devices;
    private final long startTime = System.currentTimeMillis(); // Record start time


    public SNMPRequestHandler(VirtualDevice devices) {
        this.devices = devices;
    }
    // Process a request and return a response
    public String handleRequest(String request) {
        LSNMPMessage message = LSNMPMessage.parse(request);

        // Handle GET requests
        if (message.getType() == 'G') {
            return handleGetRequest(message);
        }
        // Handle SET requests
        else if (message.getType() == 'S') {
            return handleSetRequest(message);
        }
        // Invalid request type
        else {
            throw new IllegalArgumentException("Unsupported request type: " + message.getType());
        }
    }

    // Handle GET requests
    private String handleGetRequest(LSNMPMessage request) {
        String timestamp = TimestampUtil.getUptimeTimestamp(startTime); // Uptime timestamp
        LSNMPMessage response = new LSNMPMessage('R', timestamp, request.getMessageId());

        for (String iid : request.getIidList()) {
            Object value = mib.get(iid);
            if (value != null) {
                response.addValue(value.toString());
                response.addError("0"); // No error
            } else {
                response.addError("1"); // Invalid OID
            }
        }

        return response.serialize();
    }

    // Handle SET requests
    private String handleSetRequest(LSNMPMessage request) {
        String timestamp = TimestampUtil.getUptimeTimestamp(startTime);
        LSNMPMessage response = new LSNMPMessage('R', timestamp, request.getMessageId());

        for (int i = 0; i < request.getIidList().size(); i++) {
            String iid = request.getIidList().get(i);
            String value = request.getValueList().get(i);

            // Determine handler based on OID
            String errorCode;
            if (iid.startsWith("1.1")) {
                errorCode = handleLightingRequest(iid, value);
            } else if (iid.startsWith("1.3")) {
                errorCode = handleACRequest(iid, value);
            } else {
                errorCode = "1"; // Invalid OID
            }

            // Add response
            response.addValue(value);
            response.addError(errorCode);
        }

        return response.serialize();
    }


    private String handleLightingRequest(String iid, String value) {
        try {
            // Validate OID (1.1.X)
            if (!iid.startsWith("1.1") || iid.split("\\.").length != 3) {
                throw new IllegalArgumentException("Invalid lighting OID");
            }

            // Validate value (0-100)
            int intensity = Integer.parseInt(value);
            if (intensity < 0 || intensity > 100) {
                throw new IllegalArgumentException("Light intensity must be between 0 and 100");
            }

            // Update MIB and VirtualDevice
            mib.set(iid, intensity);
            devices.setLightIntensity(iid, intensity);
            return "0"; // Success
        } catch (Exception e) {
            return "1"; // Failure
        }
    }

    private String handleACRequest(String iid, String value) {
        try {
            // Validate OID (1.3.X = mode, 1.3.X.1 = target temp)
            if (!iid.startsWith("1.3")) {
                throw new IllegalArgumentException("Invalid AC OID");
            }

            // Handle AC mode (1.3.X)
            if (iid.split("\\.").length == 3) {
                int mode = Integer.parseInt(value);
                if (mode < 0 || mode > 2) {
                    throw new IllegalArgumentException("Invalid AC mode");
                }
                mib.set(iid, mode);
                devices.setACState(iid, mode);
                return "0"; // Success
            }
            // Handle target temperature (1.3.X.1)
            else if (iid.split("\\.").length == 4) {
                double temp = Double.parseDouble(value);
                if (temp < 10 || temp > 40) {
                    throw new IllegalArgumentException("Invalid temperature");
                }
                mib.set(iid, temp);
                devices.setACTargetTemp(iid, temp);
                return "0"; // Success
            }
            else {
                throw new IllegalArgumentException("Invalid AC OID");
            }
        } catch (Exception e) {
            return "1"; // Failure
        }
    }
}