package com.pdfviewer.application;

import com.pdfviewer.model.repository.db.SqliteBookmarkRepository;
import com.pdfviewer.model.repository.db.SqliteConnectionManager;
import com.pdfviewer.model.repository.db.SqliteRecentFileRepository;
import com.pdfviewer.model.service.PdfService;
import com.pdfviewer.model.service.PdfServiceImpl;
import com.pdfviewer.view.PdfViewerFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;

/**
 * Main application class for the PDF Viewer.
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // Disable macOS input method framework to prevent TSM warnings
        // This prevents "TSM AdjustCapsLockLEDForKeyTransitionHandling" and 
        // "error messaging the mach port for IMKCFRunLoopWakeUpReliable" messages
        System.setProperty("apple.awt.enabledOnDemand", "true");

        // Initialize the application on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                // Set the look and feel to the system look and feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

                // Create the PDF service
                PdfService pdfService = new PdfServiceImpl();

                // Create the database connection manager
                SqliteConnectionManager connectionManager = new SqliteConnectionManager();

                // Create the repositories
                SqliteBookmarkRepository bookmarkRepository = new SqliteBookmarkRepository(connectionManager);
                SqliteRecentFileRepository recentFileRepository = new SqliteRecentFileRepository(connectionManager);

                // Create the main application frame
                PdfViewerFrame frame = new PdfViewerFrame(pdfService, bookmarkRepository, null, recentFileRepository);

                // Show the frame
                frame.setVisible(true);

                // If command line arguments include a file path, open it
                if (args.length > 0) {
                    File file = new File(args[0]);
                    if (file.exists() && file.isFile() && file.getName().toLowerCase().endsWith(".pdf")) {
                        frame.openDocument(file);
                    }
                }

                logger.info("PDF Viewer application started");

            } catch (Exception e) {
                logger.error("Error initializing application", e);
                JOptionPane.showMessageDialog(null, "Error initializing application: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
