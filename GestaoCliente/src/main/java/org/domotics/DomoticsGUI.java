package org.domotics;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.SocketException;
import java.util.List;

public class DomoticsGUI {
    private final LSNMPManager manager;
    private JFrame frame;
    private JTextArea statusArea;
    private JComboBox<String> zoneComboBox;

    public DomoticsGUI(LSNMPManager manager) {
        this.manager = manager;
        initialize();
    }

    private void initialize() {
        // Create the main window
        frame = new JFrame("Domotics Manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);
        frame.setLayout(new BorderLayout());

        // Zone selection panel
        JPanel zonePanel = new JPanel();
        zoneComboBox = new JComboBox<>(new String[]{"Sala", "Quarto", "Cozinha"});
        zonePanel.add(new JLabel("Select Zone:"));
        zonePanel.add(zoneComboBox);
        frame.add(zonePanel, BorderLayout.NORTH);

        // Status display area
        /*statusArea = new JTextArea();
        statusArea.setEditable(false);
        statusArea.setFont(new Font("Sans-Serif", Font.PLAIN, 14));
        statusArea.setBorder(BorderFactory.createTitledBorder("Zone Status"));

        frame.add(new JScrollPane(statusArea), BorderLayout.CENTER);*/
        // Status display area with improved styling
        statusArea = new JTextArea();
        statusArea.setEditable(false);
        statusArea.setFont(new Font("Sans-Serif", Font.PLAIN, 14)); // Monospaced font for structured look
        statusArea.setBackground(new Color(240, 240, 240)); // Light gray background
        statusArea.setForeground(new Color(50, 50, 50)); // Dark text
        statusArea.setBorder(BorderFactory.createMatteBorder(5, 10, 5, 10, Color.DARK_GRAY)); // Thicker sides

        // Wrap in a panel with padding
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Outer padding
        statusPanel.add(new JScrollPane(statusArea), BorderLayout.CENTER);
        frame.add(statusPanel, BorderLayout.CENTER);


        // Control panel
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(2, 1));

        // Lighting controls
        JPanel lightingPanel = new JPanel();
        lightingPanel.setBorder(BorderFactory.createTitledBorder("Lighting"));
        JButton turnOnButton = new JButton("Turn On");
        JButton turnOffButton = new JButton("Turn Off");
        JTextField intensityField = new JTextField(5);
        JButton setIntensityButton = new JButton("Set Intensity");
        lightingPanel.add(turnOnButton);
        lightingPanel.add(turnOffButton);
        lightingPanel.add(new JLabel("Intensity (0-100):"));
        lightingPanel.add(intensityField);
        lightingPanel.add(setIntensityButton);
        controlPanel.add(lightingPanel);

        // AC controls
        JPanel acPanel = new JPanel();
        acPanel.setBorder(BorderFactory.createTitledBorder("AC"));
        JButton acOffButton = new JButton("Turn Off");
        JButton heatButton = new JButton("Heat");
        JButton coolButton = new JButton("Cool");
        JTextField tempField = new JTextField(5);
        JButton setTempButton = new JButton("Set Temp");
        acPanel.add(acOffButton);
        acPanel.add(heatButton);
        acPanel.add(coolButton);
        acPanel.add(new JLabel("Temp (10-40°C):"));
        acPanel.add(tempField);
        acPanel.add(setTempButton);
        controlPanel.add(acPanel);

        frame.add(controlPanel, BorderLayout.SOUTH);

        // Event listeners
        // Automatically refresh status when zone is changed
        zoneComboBox.addActionListener(e -> refreshStatus());
        turnOnButton.addActionListener(e -> setLightState(1));
        turnOffButton.addActionListener(e -> setLightState(0));
        setIntensityButton.addActionListener(e -> setLightIntensity(intensityField.getText()));
        acOffButton.addActionListener(e -> setACState(0));
        heatButton.addActionListener(e -> setACState(1));
        coolButton.addActionListener(e -> setACState(2));
        setTempButton.addActionListener(e -> setACTargetTemp(tempField.getText()));

        // Display the window
        frame.setVisible(true);
        refreshStatus();
    }

    private void refreshStatus() {
        int zone = zoneComboBox.getSelectedIndex() + 1; // Zones are 1, 2, 3
        try {
            LSNMPMessage response = manager.sendGetRequest(List.of(
                    "1.1." + zone,       // Zone X Light
                    "1.2." + zone,       // Zone X Temperature
                    "1.3." + zone,       // Zone X AC Mode
                    "1.3." + zone + ".1" // Zone X AC Target Temp
            ));

            String lightStatus = response.getValueList().get(0);
            String temperature = response.getValueList().get(1);
            String acStatus = response.getValueList().get(2);
            String acTargetTemp = response.getValueList().get(3);

            statusArea.setText("Status for Zone " + zone + ":\n" +
                    "- Light: " + lightStatus + "%\n" +
                    "- Temperature: " + temperature + "°C\n" +
                    "- AC: " + parseACStatus(acStatus) + "\n" +
                    "- AC Target Temp: " + acTargetTemp + "°C");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error fetching status: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setLightState(int status) {
        int zone = zoneComboBox.getSelectedIndex() + 1;
        if(status == 1){status = 100;}
        try {
            manager.sendSetRequest("1.1." + zone, String.valueOf(status));
            refreshStatus();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error setting light state: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setLightIntensity(String intensity) {
        int zone = zoneComboBox.getSelectedIndex() + 1;
        try {
            int value = Integer.parseInt(intensity);
            if (value < 0 || value > 100) {
                throw new IllegalArgumentException("Intensity must be between 0 and 100");
            }
            manager.sendSetRequest("1.1." + zone, String.valueOf(value));
            refreshStatus();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Invalid intensity: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setACState(int mode) {
        int zone = zoneComboBox.getSelectedIndex() + 1;
        try {
            manager.sendSetRequest("1.3." + zone, String.valueOf(mode));
            refreshStatus();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error setting AC state: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setACTargetTemp(String temp) {
        int zone = zoneComboBox.getSelectedIndex() + 1;
        try {
            double value = Double.parseDouble(temp);
            if (value < 10 || value > 40) {
                throw new IllegalArgumentException("Temperature must be between 10°C and 40°C");
            }
            manager.sendSetRequest("1.3." + zone + ".1", String.valueOf(value));
            refreshStatus();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Invalid temperature: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String parseACStatus(String status) {
        return switch (status) {
            case "0" -> "OFF";
            case "1" -> "HEAT";
            case "2" -> "COOL";
            default -> "UNKNOWN";
        };
    }

    public static void main(String[] args) {
        try {
            LSNMPManager manager = new LSNMPManager("localhost", 161);
            new DomoticsGUI(manager);
        } catch (SocketException e) {
            JOptionPane.showMessageDialog(null, "Failed to initialize manager: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
