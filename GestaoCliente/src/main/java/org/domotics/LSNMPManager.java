package org.domotics;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;

public class LSNMPManager {
    private final String agentHost;
    private final int agentPort;
    private final DatagramSocket socket; // Reuse the same socket

    public LSNMPManager(String agentHost, int agentPort) throws SocketException {
        this.agentHost = agentHost;
        this.agentPort = agentPort;
        this.socket = new DatagramSocket(162); // Bind to port 162
        this.socket.setSoTimeout(5000); // Timeout after 5 seconds
    }

    // Send a GET request for one or more IIDs
    public LSNMPMessage sendGetRequest(List<String> iids) throws IOException {
        String timestamp = TimestampUtil.getCurrentTimestamp(); // Dynamic timestamp
        LSNMPMessage request = new LSNMPMessage('G', timestamp, "manager123");
        for (String iid : iids) {
            request.addIid(iid);
        }
        return sendRequest(request);
    }

    public LSNMPMessage sendSetRequest(String iid, String value) throws IOException {
        String timestamp = TimestampUtil.getCurrentTimestamp(); // Dynamic timestamp
        LSNMPMessage request = new LSNMPMessage('S', timestamp, "manager123");
        request.addIid(iid);
        request.addValue(value);
        return sendRequest(request);
    }

    // Send a request and wait for a response
    // Send a request and wait for a response
    public LSNMPMessage sendRequest(LSNMPMessage request) throws IOException {
        String serialized = request.serialize();
        byte[] data = serialized.getBytes();

        // Send request to Agent
        InetAddress address = InetAddress.getByName(agentHost);
        DatagramPacket packet = new DatagramPacket(data, data.length, address, agentPort);
        socket.send(packet);

        // Wait for response on the same socket
        byte[] buffer = new byte[1024];
        DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
        socket.receive(responsePacket);

        return LSNMPMessage.parse(new String(buffer, 0, responsePacket.getLength()));
    }

    private LSNMPMessage waitForResponse() throws IOException {
        try (DatagramSocket socket = new DatagramSocket(162)) { // Manager listens on port 162
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            System.out.println("Waiting for response...");
            socket.receive(packet);
            String response = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Received response: " + response);
            return LSNMPMessage.parse(response);
        }
    }

    public void close() {
        socket.close();
    }
}