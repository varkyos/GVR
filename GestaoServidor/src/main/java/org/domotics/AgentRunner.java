package org.domotics;

import java.net.SocketException;

public class AgentRunner {
    public static void main(String[] args) throws SocketException {
        VirtualDevices.Environment.startMonitoring();
        new LSNMPAgent().start();
        System.out.println("L-SNMPvS Agent started on port 161");
    }
}