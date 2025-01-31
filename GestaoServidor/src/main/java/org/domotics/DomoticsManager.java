package org.domotics;

import java.util.InputMismatchException;
import java.util.Scanner;

public class DomoticsManager {
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        while(true) {
            System.out.println("\nDomotics Manager");
            System.out.println("1. View status");
            System.out.println("2. Control lighting");
            System.out.println("3. Control AC");
            System.out.println("4. Exit");

            int choice = getIntInput("Enter choice: ", 1, 4);

            switch(choice) {
                case 1:
                    displayStatus();
                    break;
                case 2:
                    controlLighting();
                    break;
                case 3:
                    controlAC();
                    break;
                case 4:
                    System.exit(0);
            }
        }
    }

    private static void displayStatus() {
        DomoticsMIB mib = DomoticsMIB.getInstance();
        System.out.println("\nCurrent Status:");

        mib.getAllOIDs().forEach(oid -> {
            Object value = mib.getValue(oid);
            System.out.printf("%-15s: %s%n", oid, value);
        });
    }

    private static void controlLighting() {
        String zone = getStringInput("Enter zone number: ");

        System.out.println("1. Turn On\n2. Turn Off\n3. Set Intensity");
        int choice = getIntInput("Enter choice: ", 1, 3);

        switch(choice) {
            case 1:
                VirtualDevices.Lighting.setPower(zone, true);
                break;
            case 2:
                VirtualDevices.Lighting.setPower(zone, false);
                break;
            case 3:
                int level = getIntInput("Enter intensity (0-100): ", 0, 100);
                VirtualDevices.Lighting.setIntensity(zone, level);
                break;
        }
    }

    private static void controlAC() {
        String zone = getStringInput("Enter zone number: ");

        System.out.println("1. Turn Off\n2. Heat Mode\n3. Cool Mode\n4. Set Temp");
        int choice = getIntInput("Enter choice: ", 1, 4);

        switch(choice) {
            case 1:
                VirtualDevices.AirConditioning.setMode(zone, "off");
                break;
            case 2:
                VirtualDevices.AirConditioning.setMode(zone, "heat");
                break;
            case 3:
                VirtualDevices.AirConditioning.setMode(zone, "cool");
                break;
            case 4:
                double temp = getDoubleInput("Enter temperature (10-40°C): ", 10, 40);
                VirtualDevices.AirConditioning.setTargetTemp(zone, temp);
                break;
        }
    }

    // Validation helper methods
    private static int getIntInput(String prompt, int min, int max) {
        while(true) {
            try {
                System.out.print(prompt);
                int input = scanner.nextInt();
                scanner.nextLine(); // Clear buffer

                if(input >= min && input <= max) {
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
        while(true) {
            try {
                System.out.print(prompt);
                double input = scanner.nextDouble();
                scanner.nextLine(); // Clear buffer

                if(input >= min && input <= max) {
                    return input;
                }
                System.out.printf("Invalid input! Must be between %.1f-%.1f. Try again.\n", min, max);
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