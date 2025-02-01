package org.domotics;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class LSNMPAgent {

    private final ExecutorService threadPool;
    private final UDPListener listener;
    private final long startTime = System.currentTimeMillis(); // Record start time

    public LSNMPAgent(int port) {
        this.threadPool = Executors.newCachedThreadPool(); // Create a thread pool
        VirtualDevice devices = new VirtualDevice();
        SNMPRequestHandler handler = new SNMPRequestHandler(devices);
        this.listener = new UDPListener(port, handler, threadPool);
    }

    public void start() {
        System.out.println("Starting LSNMP Agent...");
        listener.listen();
    }

    public void stop() {
        threadPool.shutdown(); // Shut down the thread pool
        System.out.println("LSNMP Agent stopped.");
    }

    public static void main(String[] args) {
        LSNMPAgent agent = new LSNMPAgent(161);
        agent.start();
    }
}
