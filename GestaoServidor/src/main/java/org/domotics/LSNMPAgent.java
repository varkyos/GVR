package org.domotics;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LSNMPAgent {
    private static final int PORT = 161;
    private final DatagramSocket socket;
    private final DomoticsMIB mib;

    // Add debug flag
    public static final boolean DBUG = true; // Set to false for production

    public LSNMPAgent() throws SocketException {
        this.socket = new DatagramSocket(PORT);
        this.mib = DomoticsMIB.getInstance();
    }

    public void start() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            while(true) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    // Get client IP
                    InetAddress clientAddress = packet.getAddress();
                    int clientPort = packet.getPort();

                    if(DBUG) {
                        System.out.println("\n[DEBUG] Received from " +
                                clientAddress.getHostAddress() + ":" + clientPort);
                    }

                    // Process with client IP
                    String response = processRequest(
                            new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8),
                            clientAddress
                    );

                    // Send response
                    byte[] resData = response.getBytes(StandardCharsets.UTF_8);
                    DatagramPacket resPacket = new DatagramPacket(
                            resData, resData.length, clientAddress, clientPort
                    );
                    socket.send(resPacket);

                } catch (IOException e) {
                    if(DBUG) e.printStackTrace();
                }
            }
        }).start();
    }

    private String processRequest(String request, InetAddress clientAddress) {
        try {
            LSNMPMessage message = LSNMPMessage.parse(request.getBytes(StandardCharsets.UTF_8));

            if(message.getType() == 'G') { // GET
                List<String> oids = message.getIidList();
                List<LSNMPMessage.ValueEntry> values = new ArrayList<>();

                for(String oid : oids) {
                    Object value = mib.getValue(oid);
                    values.add(new LSNMPMessage.ValueEntry(
                            getDataType(value),
                            getDataLength(value),
                            value.toString()
                    ));
                }

                return new String(buildResponse(message, values), StandardCharsets.UTF_8);

            } else if(message.getType() == 'S') { // SET
                return new String(buildAckResponse(message), StandardCharsets.UTF_8);
            }

        } catch (Exception e) {
            return new String(buildErrorResponse(e.getMessage()), StandardCharsets.UTF_8);
        }
        return "";
    }

    private byte[] buildAckResponse(LSNMPMessage request) {
        LSNMPMessage response = new LSNMPMessage();
        response.setType('R');
        response.setTimestamp(generateTimestamp());
        response.setMessageId(request.getMessageId());
        response.getErrorList().add(0); // Success code
        return response.serialize();
    }

    private byte[] buildErrorResponse(String errorMessage) {
        LSNMPMessage response = new LSNMPMessage();
        response.setType('R');
        response.setTimestamp(generateTimestamp());
        response.getErrorList().add(1); // Error code
        response.getValueList().add(new LSNMPMessage.ValueEntry(
                LSNMPMessage.DataType.S,
                errorMessage.length(),
                errorMessage
        ));
        return response.serialize();
    }

    private String generateTimestamp() {
        // Agent timestamp format: days:hours:minutes:seconds:ms (uptime)
        // For simplicity, using current time (replace with actual uptime calculation)
        return DateTimeFormatter.ofPattern("dd:MM:yyyy:HH:mm:ss:SSS")
                .format(LocalDateTime.now());
    }

    private byte[] buildResponse(LSNMPMessage request, List<LSNMPMessage.ValueEntry> values) {
        LSNMPMessage response = new LSNMPMessage();
        response.setType('R');
        response.setTimestamp(generateTimestamp());
        response.setMessageId(request.getMessageId());
        response.getIidList().addAll(request.getIidList());
        response.getValueList().addAll(values);
        return response.serialize();
    }

    private LSNMPMessage.DataType getDataType(Object value) {
        if(value instanceof Integer) return LSNMPMessage.DataType.I;
        if(value instanceof Double) return LSNMPMessage.DataType.D;
        return LSNMPMessage.DataType.S;
    }

    private int getDataLength(Object value) {
        if(value instanceof Integer) return 1;
        if(value instanceof Double) return 1;
        return value.toString().length();
    }
}