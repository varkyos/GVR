package org.domotics;

import java.io.IOException;
import java.net.SocketException;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

public class DomoticsManager {
    private static final Scanner scanner = new Scanner(System.in);
    private static LSNMPManager manager;

    public static void main(String[] args) {
        try {
            manager = new LSNMPManager("localhost", 161); // Agent's port
        while (true) {
            System.out.println("\nDomotics Manager");
            System.out.println("1. View status");
            System.out.println("2. Control lighting");
            System.out.println("3. Control AC");
            System.out.println("4. Exit");

            int choice = getIntInput("Enter choice: ", 1, 4);

            switch (choice) {
                case 1 -> displayStatus();
                case 2 -> controlLighting();
                case 3 -> controlAC();
                case 4 -> System.exit(0);
            }
        }
        } catch (SocketException e) {
            System.err.println("Failed to initialize manager: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error during communication: " + e.getMessage());
        }
    }

    private static void displayStatus() {
        String zone = getStringInput("Enter zone number (1, 2, or 3): ");
        if (!zone.matches("[1-3]")) {
            System.out.println("Invalid zone number. Please choose 1, 2, or 3.");
            return;
        }

        try {
            // Fetch status for the selected zone
            LSNMPMessage response = manager.sendGetRequest(List.of(
                    "1.1." + zone,       // Zone X Light
                    "1.2." + zone,       // Zone X Temperature
                    "1.3." + zone,       // Zone X AC Mode
                    "1.3." + zone + ".1" // Zone X AC Target Temp
            ));

            // Parse response
            String lightIntensity = response.getValueList().get(0);
            String temperature = response.getValueList().get(1);
            String acStatus = response.getValueList().get(2);
            String acTargetTemp = response.getValueList().get(3);

            // Display status
            System.out.println("\nStatus for Zone " + zone + ":");
            System.out.println("- Light: " + lightIntensity + "%");
            System.out.println("- Temperature: " + temperature + "°C");
            System.out.println("- AC: " + parseACStatus(acStatus));
            System.out.println("- AC Target Temp: " + acTargetTemp + "°C");
        } catch (IOException e) {
            System.err.println("Error fetching status: " + e.getMessage());
        }
    }

    private static void controlLighting() {
        String zone = getStringInput("Enter zone number: ");
        String lightOID = "1.1." + zone;

        System.out.println("1. Turn On\n2. Turn Off\n3. Set Intensity");
        int choice = getIntInput("Enter choice: ", 1, 3);

        switch (choice) {
            case 1 -> sendSetRequest(lightOID, "100"); // Turn on (100%)
            case 2 -> sendSetRequest(lightOID, "0");   // Turn off (0%)
            case 3 -> {
                int intensity = getIntInput("Enter intensity (0-100): ", 0, 100);
                sendSetRequest(lightOID, String.valueOf(intensity)); // Set intensity
            }
        }
    }

    private static void controlAC() {
        String zone = getStringInput("Enter zone number: ");
        String acOID = "1.3." + zone;

        System.out.println("1. Turn Off\n2. Heat Mode\n3. Cool Mode\n4. Set Temp");
        int choice = getIntInput("Enter choice: ", 1, 4);

        switch (choice) {
            case 1 -> sendSetRequest(acOID, "0");
            case 2 -> sendSetRequest(acOID, "1");
            case 3 -> sendSetRequest(acOID, "2");
            case 4 -> {
                double temp = getDoubleInput("Enter temperature (10-40°C): ", 10, 40);
                sendSetRequest(acOID + ".1", String.valueOf(temp)); // Target temp OID
            }
        }
    }

    // Helper method to send GET requests
    private static String sendGetRequest(String oid) {
        try {
            LSNMPMessage response = manager.sendGetRequest(List.of(oid));
            return response.getValueList().get(0);
        } catch (Exception e) {
            System.err.println("Error fetching data: " + e.getMessage());
            return "N/A";
        }
    }

    // Helper method to send SET requests
    private static void sendSetRequest(String oid, String value) {
        try {
            LSNMPMessage response = manager.sendSetRequest(oid, value);
            if (response.getErrorList().get(0).equals("0")) {
                System.out.println("Operation successful!");
            } else {
                System.out.println("Operation failed: Invalid OID or value");
            }
        } catch (Exception e) {
            System.err.println("Error sending command: " + e.getMessage());
        }
    }

    // Helper method to parse AC status
    private static String parseACStatus(String status) {
        return switch (status) {
            case "0" -> "OFF";
            case "1" -> "HEAT";
            case "2" -> "COOL";
            default -> "UNKNOWN";
        };
    }

    // Validation helper methods
    private static int getIntInput(String prompt, int min, int max) {
        while (true) {
            try {
                System.out.print(prompt);
                int input = scanner.nextInt();
                scanner.nextLine(); // Clear buffer

                if (input >= min && input <= max) {
                    return input;
                }
                System.out.printf("Invalid input! Must be between %d-%d. Try again.\n", min, max);
            } catch (InputMismatchException e) {
                System.out.println("Invalid input! Please enter a number.");
                scanner.nextLine(); // Clear invalid input
            }
        }
    }

    private static double getDoubleInput(String prompt, double min, double max) {
        while (true) {
            try {
                System.out.print(prompt);
                double input = scanner.nextDouble();
                scanner.nextLine(); // Clear buffer

                if (input >= min && input <= max) {
                    return input;
                }
                System.out.printf("Invalid input! Must be between %.0fºC-%.0fºC. Try again.\n", min, max);
            } catch (InputMismatchException e) {
                System.out.println("Invalid input! Please enter a number.");
                scanner.nextLine(); // Clear invalid input
            }
        }
    }

    private static String getStringInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }
}