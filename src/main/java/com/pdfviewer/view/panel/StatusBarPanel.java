package com.pdfviewer.view.panel;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Status bar panel for displaying application status messages.
 */
public class StatusBarPanel extends JPanel {
    private final JLabel statusLabel;
    private final JLabel timeLabel;
    private final Timer timer;
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    /**
     * Creates a new status bar panel.
     */
    public StatusBarPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEtchedBorder());
        
        // Status message label (left-aligned)
        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        
        // Time label (right-aligned)
        timeLabel = new JLabel();
        timeLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        updateTimeLabel();
        
        // Create a timer to update the time every second
        timer = new Timer(1000, e -> updateTimeLabel());
        timer.start();
        
        // Add components to the panel
        add(statusLabel, BorderLayout.WEST);
        add(timeLabel, BorderLayout.EAST);
    }
    
    /**
     * Sets the status message.
     *
     * @param message The status message to display
     */
    public void setMessage(String message) {
        statusLabel.setText(message);
    }
    
    /**
     * Gets the current status message.
     *
     * @return The current status message
     */
    public String getMessage() {
        return statusLabel.getText();
    }
    
    /**
     * Updates the time label with the current time.
     */
    private void updateTimeLabel() {
        timeLabel.setText(LocalDateTime.now().format(TIME_FORMATTER));
    }
    
    /**
     * Cleans up resources when the panel is no longer needed.
     */
    public void cleanup() {
        timer.stop();
    }
}