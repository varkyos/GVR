package org.domotics;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LSNMPMessage {
    // Protocol constants
    public static final String PROTOCOL_TAG = "kdk847ufh84jg87g";
    public static final List<Character> VALID_TYPES = List.of('G', 'S', 'N', 'R');
    public static final List<String> VALID_ERRORS = List.of("0", "1", "2", "3", "4");

    // Message components
    private char type;
    private String timestamp;
    private String messageId;
    private String clientIP;
    private final List<String> iidList = new ArrayList<>();
    private final List<ValueEntry> valueList = new ArrayList<>();
    private final List<Integer> errorList = new ArrayList<>();

    // Value type handling
    public enum DataType { I, T, S, D }

    public static class ValueEntry {
        private final DataType type;
        private final int length;
        private final String value;

        public ValueEntry(DataType type, int length, String value) {
            this.type = Objects.requireNonNull(type);
            this.length = length;
            this.value = Objects.requireNonNull(value);
            validate();
        }

        private void validate() {
            switch (type) {
                case I:
                    if (!value.matches("^-?\\d+$")) {
                        throw new ProtocolException("Invalid integer value: " + value);
                    }
                    if (length != 1) {
                        throw new ProtocolException("Integer values must have length=1");
                    }
                    break;
                case T:
                    int componentCount = value.split(":").length;
                    if (componentCount != 5 && componentCount != 7) {
                        throw new ProtocolException("Invalid timestamp format: " + value);
                    }
                    if (length != componentCount) {
                        throw new ProtocolException("Timestamp length mismatch");
                    }
                    break;
                case D:
                    if (!value.matches("^\\d+(\\.\\d+)*$")) {
                        throw new ProtocolException("Invalid IID format: " + value);
                    }
                    break;
            }
        }

        // Getters
        public DataType getType() { return type; }
        public int getLength() { return length; }
        public String getValue() { return value; }
    }

    // Custom exception type
    public static class ProtocolException extends RuntimeException {
        public ProtocolException(String message) {
            super("L-SNMPvS Protocol Violation: " + message);
        }
    }

    public static LSNMPMessage parse(byte[] data, InetAddress clientAddress) {
        LSNMPMessage message = parse(data);
        message.clientIP = clientAddress.getHostAddress();
        return message;
    }

    // Parser implementation
    public static LSNMPMessage parse(byte[] data) {
        String message = new String(data, StandardCharsets.UTF_8);
        String[] parts = message.split("\0", -1);
        int index = 0;
        LSNMPMessage msg = new LSNMPMessage();

        try {
            // Validate protocol tag
            if (!parts[index++].equals(PROTOCOL_TAG)) {
                throw new ProtocolException("Invalid protocol tag");
            }

            // Parse message type
            msg.type = parseType(parts[index++]);

            // Parse timestamp
            msg.timestamp = parseTimestamp(parts[index++]);

            // Parse message identifier
            msg.messageId = parts[index++];
            if (msg.messageId.isEmpty()) {
                throw new ProtocolException("Missing message identifier");
            }

            // Parse IID list
            msg.iidList.addAll(parseIidList(parts, index));
            index += msg.iidList.size() + 1;

            // Parse value list
            msg.valueList.addAll(parseValueList(parts, index));
            index += msg.valueList.size() * 3 + 1;

            // Parse error list
            msg.errorList.addAll(parseErrorList(parts, index));

        } catch (IndexOutOfBoundsException e) {
            throw new ProtocolException("Malformed message structure");
        }

        return msg;
    }

    // Serializer implementation
    public byte[] serialize() {
        validateState();
        StringBuilder sb = new StringBuilder();

        sb.append(PROTOCOL_TAG).append('\0');
        sb.append(type).append('\0');
        sb.append(timestamp).append('\0');
        sb.append(messageId).append('\0');

        // IID list
        sb.append(iidList.size()).append('\0');
        for (String iid : iidList) {
            sb.append(iid).append('\0');
        }

        // Value list
        sb.append(valueList.size()).append('\0');
        for (ValueEntry entry : valueList) {
            sb.append(entry.type.name())
                    .append('|')
                    .append(entry.length)
                    .append('|')
                    .append(entry.value)
                    .append('\0');
        }

        // Error list
        sb.append(errorList.size()).append('\0');
        for (Integer error : errorList) {
            sb.append(error).append('\0');
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // Validation helpers
    private void validateState() {
        if (!VALID_TYPES.contains(type)) {
            throw new ProtocolException("Invalid message type: " + type);
        }

        if (timestamp.split(":").length != 5 && timestamp.split(":").length != 7) {
            throw new ProtocolException("Invalid timestamp format");
        }

        for (String iid : iidList) {
            if (iid.split("\\.").length < 2) {
                throw new ProtocolException("IID must contain at least two components");
            }
        }
    }

    // Parsing helpers
    private static char parseType(String typeStr) {
        if (typeStr.length() != 1 || !VALID_TYPES.contains(typeStr.charAt(0))) {
            throw new ProtocolException("Invalid message type: " + typeStr);
        }
        return typeStr.charAt(0);
    }

    private static String parseTimestamp(String ts) {
        if (ts.isEmpty()) {
            throw new ProtocolException("Missing timestamp");
        }
        return ts;
    }

    private static List<String> parseIidList(String[] parts, int index) {
        List<String> iids = new ArrayList<>();
        int count = Integer.parseInt(parts[index++]);

        for (int i = 0; i < count; i++) {
            String iid = parts[index + i];
            if (!iid.matches("^\\d+(\\.\\d+)*$")) {
                throw new ProtocolException("Invalid IID format: " + iid);
            }
            if (iid.split("\\.").length < 2) {
                throw new ProtocolException("IID must contain at least two components");
            }
            iids.add(iid);
        }
        return iids;
    }

    private static List<ValueEntry> parseValueList(String[] parts, int index) {
        List<ValueEntry> values = new ArrayList<>();
        int count = Integer.parseInt(parts[index++]);

        for (int i = 0; i < count; i++) {
            int base = index + (i * 3);
            DataType type = DataType.valueOf(parts[base]);
            int length = Integer.parseInt(parts[base + 1]);
            String value = parts[base + 2];
            values.add(new ValueEntry(type, length, value));
        }
        return values;
    }

    private static List<Integer> parseErrorList(String[] parts, int index) {
        List<Integer> errors = new ArrayList<>();
        int count = Integer.parseInt(parts[index++]);

        for (int i = 0; i < count; i++) {
            String error = parts[index + i];
            if (!VALID_ERRORS.contains(error)) {
                throw new ProtocolException("Invalid error code: " + error);
            }
            errors.add(Integer.parseInt(error));
        }
        return errors;
    }

    // Getters and setters with validation
    public char getType() { return type; }

    public void setType(char type) {
        if (!VALID_TYPES.contains(type)) {
            throw new IllegalArgumentException("Invalid message type");
        }
        this.type = type;
    }

    public String getTimestamp() { return timestamp; }

    public void setTimestamp(String timestamp) {
        if (timestamp.split(":").length != 5 && timestamp.split(":").length != 7) {
            throw new IllegalArgumentException("Invalid timestamp format");
        }
        this.timestamp = timestamp;
    }

    public String getMessageId() { return messageId; }

    public void setMessageId(String messageId) {
        if (messageId == null || messageId.isEmpty()) {
            throw new IllegalArgumentException("Message ID cannot be empty");
        }
        this.messageId = messageId;
    }

    public List<String> getIidList() { return new ArrayList<>(iidList); }

    public void addIid(String iid) {
        if (!iid.matches("^\\d+(\\.\\d+)*$")) {
            throw new IllegalArgumentException("Invalid IID format");
        }
        iidList.add(iid);
    }

    public List<ValueEntry> getValueList() { return new ArrayList<>(valueList); }

    public void addValue(ValueEntry entry) {
        valueList.add(entry);
    }

    private static String validateTimestamp(String ts, boolean isAgent) {
        if(isAgent) {
            // Agent timestamp format: days:hours:minutes:seconds:ms
            if(!ts.matches("^\\d+:\\d{2}:\\d{2}:\\d{2}:\\d{3}$")) {
                throw new IllegalArgumentException("Invalid agent timestamp format");
            }
        } else {
            // Manager timestamp format: dd:mm:yyyy:hh:mm:ss:ms
            if(!ts.matches("^\\d{2}:\\d{2}:\\d{4}:\\d{2}:\\d{2}:\\d{2}:\\d{3}$")) {
                throw new IllegalArgumentException("Invalid manager timestamp format");
            }
        }
        return ts;
    }
    public List<Integer> getErrorList() { return new ArrayList<>(errorList); }

    public void addError(int error) {
        if (!VALID_ERRORS.contains(String.valueOf(error))) {
            throw new IllegalArgumentException("Invalid error code");
        }
        errorList.add(error);
    }

    // Example usage
    @Override
    public String toString() {
        return "LSNMPMessage {\n" +
                "  Client: " + clientIP + "\n" +
                "  Type: " + type + "\n" +
                "  Timestamp: " + timestamp + "\n" +
                "  MessageID: " + messageId + "\n" +
                "  IIDs: " + iidList + "\n" +
                "  Values: " + valueList + "\n" +
                "  Errors: " + errorList + "\n" +
                "}";
    }
}