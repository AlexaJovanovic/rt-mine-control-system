package com.prv.rt_system;

import javax.swing.*;

import com.prv.EnvironmentState;
import com.prv.rt_system.ControlSystem.AlarmType;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class EnvGUI {
    private JFrame frame;
    private JTextArea logArea;
    private JProgressBar waterLevelBar;
    private JTextArea alarmListArea;

    // Buttons for ADC devices and pump malfunction
    private JButton[] adcButtons;
    private String[] adc_names = { "ADC1 CO", "ADC2 CH4", "ADC3 AirFlow", "ADC4 PumpWF" };
    private JButton pumpFaultButton;
    
    // pump control button and status indicator
    private JButton pumpOnButton;
    private JButton pumpOffButton;
    private JLabel pumpStateLabel; // indicator for pump state
    
    private static EnvGUI instance;

    public static EnvGUI getInstance() {
        return instance;
    }

    public EnvGUI(ControlSystem controlSystem) {
        // Create main window
        frame = new JFrame("Environment Control System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 700);
        frame.setLayout(new BorderLayout());

        // === CENTER: Logs ===
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("System Logs"));
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        logPanel.add(scrollPane, BorderLayout.CENTER);
        frame.add(logPanel, BorderLayout.CENTER);

        // === RIGHT: Alarms ===
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Alarms"));
        rightPanel.setPreferredSize(new Dimension(250, 0)); // wider panel (250px)
        
        alarmListArea = new JTextArea("No Alarms");
        alarmListArea.setEditable(false);
        alarmListArea.setBackground(Color.GREEN);
        alarmListArea.setForeground(Color.BLACK);
        alarmListArea.setFont(new Font("Monospaced", Font.BOLD, 12));

        JScrollPane alarmScrollPane = new JScrollPane(alarmListArea);
        rightPanel.add(alarmScrollPane, BorderLayout.CENTER);

        frame.add(rightPanel, BorderLayout.EAST);

        // === LEFT: Water level visualization ===
        JPanel waterPanel = new JPanel(new BorderLayout());
        waterPanel.setBorder(BorderFactory.createTitledBorder("Water Level"));

        // Vertical progress bar (like a tank)
        waterLevelBar = new JProgressBar(SwingConstants.VERTICAL, 0, 100);
        waterLevelBar.setValue(50); // placeholder for 50% water level
        waterLevelBar.setStringPainted(true);

        // Make the bar blue like water
        waterLevelBar.setForeground(new Color(30, 144, 255)); // DodgerBlue
        waterLevelBar.setBackground(Color.WHITE);

        waterPanel.add(waterLevelBar, BorderLayout.CENTER);
        frame.add(waterPanel, BorderLayout.WEST);

        // === TOP: Control buttons (ADC + Pump malfunction) ===
        JPanel topPanel = new JPanel(new GridLayout(1, 5, 5, 5));
        topPanel.setBorder(BorderFactory.createTitledBorder("Controls"));

        adcButtons = new JButton[4];
        for (int i = 0; i < adcButtons.length; i++) {
            int idx = i;
            adcButtons[i] = new JButton(adc_names[idx] + " [OK]");
            adcButtons[i].setBackground(Color.GREEN);
            adcButtons[i].setOpaque(true);

            adcButtons[i].addActionListener(e -> toggleADCButton(adcButtons[idx], adc_names[idx], idx + 1));
            topPanel.add(adcButtons[i]);
        }

        pumpFaultButton = new JButton("Pump [OK]");
        pumpFaultButton.setBackground(Color.GREEN);
        pumpFaultButton.setOpaque(true);

        pumpFaultButton.addActionListener(e -> togglePumpButton(pumpFaultButton, "Pump"));
        topPanel.add(pumpFaultButton);
        
        frame.add(topPanel, BorderLayout.NORTH);

        // === BOTTOM: Pump controls ===
        JPanel bottomPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        bottomPanel.setBorder(BorderFactory.createTitledBorder("Pump Controls"));
        bottomPanel.setPreferredSize(new Dimension(0, 100)); // taller panel

        pumpOnButton = new JButton("Pump ON");
        pumpOffButton = new JButton("Pump OFF");
        pumpStateLabel = new JLabel("Pump is OFF", SwingConstants.CENTER);

        pumpStateLabel.setOpaque(true);
        pumpStateLabel.setBackground(Color.LIGHT_GRAY);
        pumpStateLabel.setForeground(Color.BLACK);

        // Actions
        if (controlSystem != null) {
        	pumpOnButton.addActionListener(e -> controlSystem.pumpManualControlSignal(true));
        	pumpOffButton.addActionListener(e -> controlSystem.pumpManualControlSignal(false));
        }
        
        bottomPanel.add(pumpOnButton);
        bottomPanel.add(pumpOffButton);
        bottomPanel.add(pumpStateLabel);	
        
        frame.add(bottomPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
        // ====== PLACEHOLDERS ======
        // To update alarms: alarmLabel.setText("ALARM!"); alarmLabel.setBackground(Color.RED);
        // To log: logArea.append("New log entry...\n");
        // To update water level: waterLevelBar.setValue(x);
    }
    
    private void toggleADCButton(JButton button, String name, int adcNo) {
        boolean isFault = button.getBackground() == Color.RED;

        if (isFault) {
            // Switch back to OK
            button.setBackground(Color.GREEN);
            button.setText(name + " [OK]");
            logArea.append(name + " set to OK\n");
            MCU.instance.fixADCDevice(adcNo);
        } else {
            // Switch to FAULT
            button.setBackground(Color.RED);
            button.setText(name + " [FAULT]");
            logArea.append(name + " set to FAULT\n");
            MCU.instance.brakeDownADCDevice(adcNo);
        }
    }

    private void togglePumpButton(JButton button, String name) {
        boolean isFault = button.getBackground() == Color.RED;

        if (isFault) {
            button.setBackground(Color.GREEN);
            button.setText(name + " [OK]");
            logArea.append(name + " set to OK\n");
            EnvironmentState.getInstance().setPumpWorkingProperly(true);
            
        } else {
            button.setBackground(Color.RED);
            button.setText(name + " [FAULT]");
            logArea.append(name + " set to FAULT\n");
            EnvironmentState.getInstance().setPumpWorkingProperly(false);
            
        }
    }
    
    public void updateWaterLevel(float cmValue, float minLevel, float maxLevel) {
        // Update progress bar safely on Swing thread
        SwingUtilities.invokeLater(() -> {
            waterLevelBar.setValue((int)cmValue);
            waterLevelBar.setString(String.format("%.1f cm", cmValue));
        });
    }
    
    /** Log a plain message (thread-safe). */
    public void log(String message) {
    	float t_s = EnvironmentState.getInstance().getTimeMs() / 1000;
    	
    	SwingUtilities.invokeLater(() -> {
            logArea.append(String.format("t: %5.2f s - ", t_s) + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength()); // auto-scroll
        });
    }

    /** Log a formatted message (printf style). */
    public void log(String format, Object... args) {
        String msg = String.format(format, args);
        log(msg);
    }
    
    public void setPumpState(boolean pumpOn) {
        String text = pumpOn ? "Pump is set to ON" : "Pump is set to OFF";
        SwingUtilities.invokeLater(() -> pumpStateLabel.setText(text));
    }
    
    // Alarm interface
    
    private final Set<AlarmType> activeAlarms = new HashSet<>();

    public void showAlarm(AlarmType type) {
        activeAlarms.add(type);
        updateAlarmDisplay();
    }

    public void clearAlarm(AlarmType type) {
        activeAlarms.remove(type);
        updateAlarmDisplay();
    }

    private void updateAlarmDisplay() {
        SwingUtilities.invokeLater(() -> {
            if (activeAlarms.isEmpty()) {
                alarmListArea.setText("No Alarms");
                alarmListArea.setBackground(Color.GREEN);
                alarmListArea.setForeground(Color.BLACK);
            } else {
                StringBuilder sb = new StringBuilder();
                for (AlarmType alarm : activeAlarms) {
                    sb.append("⚠ ").append(alarm).append("\n");
                }
                alarmListArea.setText(sb.toString());
                alarmListArea.setBackground(Color.YELLOW);
                alarmListArea.setForeground(Color.RED);
            }
        });
    }



    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EnvGUI(null));
    }
}
