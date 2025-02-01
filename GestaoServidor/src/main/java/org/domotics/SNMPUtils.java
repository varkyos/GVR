package org.domotics;

public class SNMPUtils {
    // Default SNMP port
    public static final int DEFAULT_PORT = 161;

    // OID prefixes
    public static final String LIGHT_OID_PREFIX = "1.1";
    public static final String TEMPERATURE_OID_PREFIX = "1.2";
    public static final String AC_OID_PREFIX = "1.3";

    // Error codes
    public static final String ERROR_NO_ERROR = "0";
    public static final String ERROR_INVALID_OID = "1";
    public static final String ERROR_INVALID_VALUE = "2";

    // Helper method to validate OIDs
    public static boolean isValidOid(String oid) {
        return oid != null && oid.matches("^\\d+(\\.\\d+)*$");
    }
}