package com.pdfviewer.view.panel;

import com.pdfviewer.model.entity.PdfDocument;
import com.pdfviewer.model.service.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Panel for displaying thumbnails of PDF pages.
 */
public class ThumbnailPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(ThumbnailPanel.class);
    
    // Constants
    private static final int THUMBNAIL_WIDTH = 150;
    private static final int THUMBNAIL_HEIGHT = 200;
    private static final int THUMBNAIL_SPACING = 10;
    
    // Services
    private final PdfService pdfService;
    private final ExecutorService thumbnailExecutor;
    
    // Document state
    private PdfDocument document;
    private int selectedPage = 1;
    
    // UI components
    private final JPanel thumbnailsContainer;
    private final List<ThumbnailItem> thumbnailItems = new ArrayList<>();
    
    // Callback for page selection
    private Consumer<Integer> pageSelectionCallback;
    
    /**
     * Creates a new thumbnail panel.
     *
     * @param pdfService The PDF service to use for generating thumbnails
     */
    public ThumbnailPanel(PdfService pdfService) {
        super(new BorderLayout());
        this.pdfService = pdfService;
        this.thumbnailExecutor = Executors.newVirtualThreadPerTaskExecutor();
        
        // Create the thumbnails container
        thumbnailsContainer = new JPanel();
        thumbnailsContainer.setLayout(new BoxLayout(thumbnailsContainer, BoxLayout.Y_AXIS));
        thumbnailsContainer.setBorder(BorderFactory.createEmptyBorder(THUMBNAIL_SPACING, THUMBNAIL_SPACING, 
                                                                     THUMBNAIL_SPACING, THUMBNAIL_SPACING));
        
        // Add the container to this panel
        add(thumbnailsContainer, BorderLayout.CENTER);
    }
    
    /**
     * Sets the document to display thumbnails for.
     *
     * @param document The PDF document
     */
    public void setDocument(PdfDocument document) {
        this.document = document;
        this.selectedPage = 1;
        generateThumbnails();
    }
    
    /**
     * Clears the current document.
     */
    public void clearDocument() {
        this.document = null;
        this.selectedPage = 1;
        thumbnailItems.clear();
        thumbnailsContainer.removeAll();
        thumbnailsContainer.revalidate();
        thumbnailsContainer.repaint();
    }
    
    /**
     * Sets the selected page.
     *
     * @param pageNumber The page number to select (1-based)
     */
    public void setSelectedPage(int pageNumber) {
        if (document == null || pageNumber < 1 || pageNumber > document.getPageCount()) {
            return;
        }
        
        if (pageNumber != selectedPage) {
            // Update selection
            int oldSelected = selectedPage;
            selectedPage = pageNumber;
            
            // Update UI if thumbnails are already generated
            if (!thumbnailItems.isEmpty()) {
                if (oldSelected > 0 && oldSelected <= thumbnailItems.size()) {
                    thumbnailItems.get(oldSelected - 1).setSelected(false);
                }
                if (selectedPage > 0 && selectedPage <= thumbnailItems.size()) {
                    thumbnailItems.get(selectedPage - 1).setSelected(true);
                    
                    // Ensure the selected thumbnail is visible
                    thumbnailItems.get(selectedPage - 1).scrollRectToVisible(
                            thumbnailItems.get(selectedPage - 1).getBounds());
                }
            }
        }
    }
    
    /**
     * Sets the callback to be invoked when a page is selected.
     *
     * @param callback The callback function
     */
    public void setPageSelectionCallback(Consumer<Integer> callback) {
        this.pageSelectionCallback = callback;
    }
    
    /**
     * Generates thumbnails for all pages in the document.
     */
    private void generateThumbnails() {
        if (document == null) {
            return;
        }
        
        // Clear existing thumbnails
        thumbnailItems.clear();
        thumbnailsContainer.removeAll();
        
        // Show loading indicator
        JLabel loadingLabel = new JLabel("Generating thumbnails...");
        loadingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        thumbnailsContainer.add(loadingLabel);
        thumbnailsContainer.revalidate();
        thumbnailsContainer.repaint();
        
        // Generate thumbnails in background
        thumbnailExecutor.submit(() -> {
            try {
                // Generate thumbnails
                List<BufferedImage> thumbnails = pdfService.generateThumbnails(
                        document, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
                
                // Update UI on EDT
                SwingUtilities.invokeLater(() -> {
                    thumbnailsContainer.removeAll();
                    thumbnailItems.clear();
                    
                    for (int i = 0; i < thumbnails.size(); i++) {
                        final int pageNumber = i + 1;
                        ThumbnailItem item = new ThumbnailItem(thumbnails.get(i), pageNumber);
                        
                        // Set selected state
                        item.setSelected(pageNumber == selectedPage);
                        
                        // Add click listener
                        item.addMouseListener(new java.awt.event.MouseAdapter() {
                            @Override
                            public void mouseClicked(java.awt.event.MouseEvent evt) {
                                setSelectedPage(pageNumber);
                                if (pageSelectionCallback != null) {
                                    pageSelectionCallback.accept(pageNumber);
                                }
                            }
                        });
                        
                        thumbnailItems.add(item);
                        thumbnailsContainer.add(item);
                        
                        // Add spacing between thumbnails
                        if (i < thumbnails.size() - 1) {
                            thumbnailsContainer.add(Box.createRigidArea(new Dimension(0, THUMBNAIL_SPACING)));
                        }
                    }
                    
                    thumbnailsContainer.revalidate();
                    thumbnailsContainer.repaint();
                    
                    logger.info("Generated {} thumbnails", thumbnails.size());
                });
                
            } catch (Exception e) {
                logger.error("Error generating thumbnails", e);
                
                // Show error on EDT
                SwingUtilities.invokeLater(() -> {
                    thumbnailsContainer.removeAll();
                    JLabel errorLabel = new JLabel("Error generating thumbnails");
                    errorLabel.setForeground(Color.RED);
                    errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                    thumbnailsContainer.add(errorLabel);
                    thumbnailsContainer.revalidate();
                    thumbnailsContainer.repaint();
                });
            }
        });
    }
    
    /**
     * Cleans up resources when the panel is no longer needed.
     */
    public void cleanup() {
        thumbnailExecutor.shutdown();
    }
    
    /**
     * Component representing a single thumbnail item.
     */
    private class ThumbnailItem extends JPanel {
        private final JLabel imageLabel;
        private final JLabel pageLabel;
        private boolean selected = false;
        
        /**
         * Creates a new thumbnail item.
         *
         * @param thumbnail The thumbnail image
         * @param pageNumber The page number
         */
        public ThumbnailItem(BufferedImage thumbnail, int pageNumber) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            setBackground(Color.WHITE);
            
            // Image label
            imageLabel = new JLabel(new ImageIcon(thumbnail));
            imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            imageLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            
            // Page number label
            pageLabel = new JLabel("Page " + pageNumber);
            pageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            // Add components
            add(imageLabel);
            add(Box.createRigidArea(new Dimension(0, 5)));
            add(pageLabel);
            
            // Set preferred size
            setPreferredSize(new Dimension(THUMBNAIL_WIDTH + 20, THUMBNAIL_HEIGHT + 40));
            
            // Make the panel selectable
            setFocusable(true);
            
            // Set initial selection state
            setSelected(selected);
        }
        
        /**
         * Sets whether this thumbnail is selected.
         *
         * @param selected true if selected, false otherwise
         */
        public void setSelected(boolean selected) {
            this.selected = selected;
            
            if (selected) {
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.BLUE, 2),
                        BorderFactory.createEmptyBorder(3, 3, 3, 3)));
                setBackground(new Color(230, 230, 255));
            } else {
                setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                setBackground(Color.WHITE);
            }
            
            repaint();
        }
        
        /**
         * Checks if this thumbnail is selected.
         *
         * @return true if selected, false otherwise
         */
        public boolean isSelected() {
            return selected;
        }
    }
}