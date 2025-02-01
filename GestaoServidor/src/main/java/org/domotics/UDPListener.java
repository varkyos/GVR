package org.domotics;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;

public class UDPListener {
    private final int port;
    private final SNMPRequestHandler handler;
    private final ExecutorService threadPool;

    public UDPListener(int port, SNMPRequestHandler handler, ExecutorService threadPool) {
        this.port = port;
        this.handler = handler;
        this.threadPool = threadPool;
    }

    public void listen() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("Listening on port " + port + "...");

            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                // Receive request
                socket.receive(packet);
                System.out.println("Received request from " + packet.getAddress() + ":" + packet.getPort());

                // Delegate to thread pool
                threadPool.submit(() -> handleRequest(socket, packet));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleRequest(DatagramSocket socket, DatagramPacket packet) {
        try {
            String request = new String(packet.getData(), 0, packet.getLength());
            String response = handler.handleRequest(request);

            // Send response to Manager's fixed port (162)
            InetAddress managerAddress = packet.getAddress();
            int managerPort = 162; // Fixed port for Manager

            byte[] responseData = response.getBytes();
            DatagramPacket responsePacket = new DatagramPacket(
                    responseData, responseData.length, managerAddress, managerPort
            );
            socket.send(responsePacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}