package org.domotics;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LSNMPMessage {
    // Protocol constants
    public static final String PROTOCOL_TAG = "kdk847ufh84jg87g";
    public static final List<Character> VALID_TYPES = List.of('G', 'S', 'N', 'R');

    // Message fields
    private char type;
    private String timestamp;
    private String messageId;
    private final List<String> iidList = new ArrayList<>();
    private final List<String> valueList = new ArrayList<>();
    private final List<String> errorList = new ArrayList<>();

    // Constructor for creating new messages
    public LSNMPMessage(char type, String timestamp, String messageId) {
        setType(type);
        setTimestamp(timestamp);
        setMessageId(messageId);
    }

    // Parse a raw message string into an LSNMPMessage object
    public static LSNMPMessage parse(String rawMessage) {
        String[] parts = rawMessage.split("\0", -1); // Split on NULL terminators
        if (parts.length < 5) {
            throw new IllegalArgumentException("Invalid message format");
        }

        // Validate protocol tag
        if (!PROTOCOL_TAG.equals(parts[0])) {
            throw new IllegalArgumentException("Invalid protocol tag");
        }

        // Parse message type
        char type = parts[1].charAt(0);
        if (!VALID_TYPES.contains(type)) {
            throw new IllegalArgumentException("Invalid message type: " + type);
        }

        // Create message object
        LSNMPMessage message = new LSNMPMessage(type, parts[2], parts[3]);

        // Parse IID list
        int iidCount = Integer.parseInt(parts[4]);
        for (int i = 0; i < iidCount; i++) {
            message.addIid(parts[5 + i]);
        }

        // Parse value list
        int valueCount = Integer.parseInt(parts[5 + iidCount]);
        for (int i = 0; i < valueCount; i++) {
            message.addValue(parts[6 + iidCount + i]);
        }

        // Parse error list
        int errorCount = Integer.parseInt(parts[6 + iidCount + valueCount]);
        for (int i = 0; i < errorCount; i++) {
            message.addError(parts[7 + iidCount + valueCount + i]);
        }

        return message;
    }

    // Serialize the message into a protocol-compliant string
    public String serialize() {
        validateState();
        StringBuilder sb = new StringBuilder();

        // Add fixed fields
        sb.append(PROTOCOL_TAG).append('\0');
        sb.append(type).append('\0');
        sb.append(timestamp).append('\0');
        sb.append(messageId).append('\0');

        // Add IID list
        sb.append(iidList.size()).append('\0');
        for (String iid : iidList) {
            sb.append(iid).append('\0');
        }

        // Add value list
        sb.append(valueList.size()).append('\0');
        for (String value : valueList) {
            sb.append(value).append('\0');
        }

        // Add error list
        sb.append(errorList.size()).append('\0');
        for (String error : errorList) {
            sb.append(error).append('\0');
        }

        return sb.toString();
    }

    // Validate message state before serialization
    private void validateState() {
        if (!VALID_TYPES.contains(type)) {
            throw new IllegalStateException("Invalid message type: " + type);
        }
        if (timestamp == null || timestamp.isEmpty()) {
            throw new IllegalStateException("Timestamp cannot be empty");
        }
        if (messageId == null || messageId.isEmpty()) {
            throw new IllegalStateException("Message ID cannot be empty");
        }
    }

    // Getters and setters
    public char getType() { return type; }
    public void setType(char type) {
        if (!VALID_TYPES.contains(type)) {
            throw new IllegalArgumentException("Invalid message type: " + type);
        }
        this.type = type;
    }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) {
        this.timestamp = Objects.requireNonNull(timestamp);
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) {
        this.messageId = Objects.requireNonNull(messageId);
    }

    public List<String> getIidList() { return new ArrayList<>(iidList); }
    public void addIid(String iid) {
        iidList.add(Objects.requireNonNull(iid));
    }

    public List<String> getValueList() { return new ArrayList<>(valueList); }
    public void addValue(String value) {
        valueList.add(Objects.requireNonNull(value));
    }

    public List<String> getErrorList() { return new ArrayList<>(errorList); }
    public void addError(String error) {
        errorList.add(Objects.requireNonNull(error));
    }
}