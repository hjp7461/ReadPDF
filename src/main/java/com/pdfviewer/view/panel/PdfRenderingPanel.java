package com.pdfviewer.view.panel;

import com.pdfviewer.model.entity.PdfDocument;
import com.pdfviewer.model.service.PdfService;
import com.pdfviewer.model.service.zoom.ZoomManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Panel for rendering and displaying PDF pages.
 */
public class PdfRenderingPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(PdfRenderingPanel.class);

    // Services
    private final PdfService pdfService;
    private final ZoomManager zoomManager;

    // Document state
    private PdfDocument document;
    private int currentPage = 1;
    private PageMode pageMode = PageMode.SINGLE_PAGE;

    // Property change support
    private final java.beans.PropertyChangeSupport propertyChangeSupport = new java.beans.PropertyChangeSupport(this);

    // UI components
    private final JPanel pagesPanel;
    private final JScrollPane scrollPane;
    private final JPanel navigationPanel;
    private final JLabel pageInfoLabel;
    private final JButton firstPageButton;
    private final JButton prevPageButton;
    private final JButton nextPageButton;
    private final JButton lastPageButton;
    private final JTextField pageNumberField;
    private JPopupMenu contextMenu;

    // Callbacks
    private Consumer<Integer> addBookmarkCallback;
    private Consumer<Integer> deleteBookmarkCallback;

    /**
     * Creates a new PDF rendering panel.
     *
     * @param pdfService The PDF service to use for rendering
     */
    public PdfRenderingPanel(PdfService pdfService) {
        super(new BorderLayout());
        this.pdfService = pdfService;
        this.zoomManager = new ZoomManager();

        // Create the pages panel
        pagesPanel = new JPanel();
        pagesPanel.setLayout(new BoxLayout(pagesPanel, BoxLayout.Y_AXIS));
        pagesPanel.setBackground(Color.DARK_GRAY);

        // Create the scroll pane
        scrollPane = new JScrollPane(pagesPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(Color.DARK_GRAY);

        // Create the navigation panel
        navigationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        // First page button
        firstPageButton = new JButton("<<");
        firstPageButton.setToolTipText("First Page");
        firstPageButton.addActionListener(e -> goToFirstPage());

        // Previous page button
        prevPageButton = new JButton("<");
        prevPageButton.setToolTipText("Previous Page");
        prevPageButton.addActionListener(e -> previousPage());

        // Page number field
        pageNumberField = new JTextField(3);
        pageNumberField.setHorizontalAlignment(JTextField.CENTER);
        pageNumberField.addActionListener(e -> {
            try {
                int page = Integer.parseInt(pageNumberField.getText());
                goToPage(page);
            } catch (NumberFormatException ex) {
                // Reset to current page if invalid input
                updatePageInfo();
            }
        });

        // Page info label
        pageInfoLabel = new JLabel(" / 0");

        // Next page button
        nextPageButton = new JButton(">");
        nextPageButton.setToolTipText("Next Page");
        nextPageButton.addActionListener(e -> nextPage());

        // Last page button
        lastPageButton = new JButton(">>");
        lastPageButton.setToolTipText("Last Page");
        lastPageButton.addActionListener(e -> goToLastPage());

        // Add components to navigation panel
        navigationPanel.add(firstPageButton);
        navigationPanel.add(prevPageButton);
        navigationPanel.add(pageNumberField);
        navigationPanel.add(pageInfoLabel);
        navigationPanel.add(nextPageButton);
        navigationPanel.add(lastPageButton);

        // Add mouse wheel listener for zooming
        addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                if (e.getWheelRotation() < 0) {
                    zoomIn();
                } else {
                    zoomOut();
                }
                e.consume();
            }
        });

        // Create context menu
        createContextMenu();

        // Add mouse listener for context menu
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }
        });

        // Add components to main panel
        add(scrollPane, BorderLayout.CENTER);
        add(navigationPanel, BorderLayout.SOUTH);

        // Initialize UI state
        updateUIState();
    }

    /**
     * Sets the document to display.
     *
     * @param document The PDF document to display
     */
    public void setDocument(PdfDocument document) {
        this.document = document;
        this.currentPage = 1;
        updateUIState();
        renderCurrentPage();
        logger.info("Document set: {}, {} pages", document.getFileName(), document.getPageCount());
    }

    /**
     * Clears the current document.
     */
    public void clearDocument() {
        this.document = null;
        this.currentPage = 1;
        pagesPanel.removeAll();
        pagesPanel.revalidate();
        pagesPanel.repaint();
        updateUIState();
        logger.info("Document cleared");
    }

    /**
     * Navigates to the specified page.
     *
     * @param pageNumber The page number to navigate to (1-based)
     */
    public void goToPage(int pageNumber) {
        if (document == null) {
            return;
        }

        if (pageNumber < 1) {
            pageNumber = 1;
        } else if (pageNumber > document.getPageCount()) {
            pageNumber = document.getPageCount();
        }

        if (pageNumber != currentPage) {
            int oldPage = currentPage;
            currentPage = pageNumber;
            renderCurrentPage();
            logger.debug("Navigated to page {}", currentPage);

            // Fire property change event
            propertyChangeSupport.firePropertyChange("currentPage", oldPage, currentPage);
        }

        updateUIState();
    }

    /**
     * Navigates to the first page.
     */
    public void goToFirstPage() {
        goToPage(1);
    }

    /**
     * Navigates to the last page.
     */
    public void goToLastPage() {
        if (document != null) {
            goToPage(document.getPageCount());
        }
    }

    /**
     * Navigates to the previous page.
     */
    public void previousPage() {
        goToPage(currentPage - 1);
    }

    /**
     * Navigates to the next page.
     */
    public void nextPage() {
        goToPage(currentPage + 1);
    }

    /**
     * Zooms in the current view.
     */
    public void zoomIn() {
        if (document != null) {
            zoomManager.zoomIn();
            renderCurrentPage();
            logger.debug("Zoomed in to {}%", zoomManager.getZoomPercentage());
        }
    }

    /**
     * Zooms out the current view.
     */
    public void zoomOut() {
        if (document != null) {
            zoomManager.zoomOut();
            renderCurrentPage();
            logger.debug("Zoomed out to {}%", zoomManager.getZoomPercentage());
        }
    }

    /**
     * Resets the zoom level to 100%.
     */
    public void resetZoom() {
        if (document != null) {
            zoomManager.resetZoom();
            renderCurrentPage();
            logger.debug("Zoom reset to 100%");
        }
    }

    /**
     * Sets the page display mode.
     *
     * @param mode The page display mode
     */
    public void setPageMode(PageMode mode) {
        if (this.pageMode != mode) {
            this.pageMode = mode;
            if (document != null) {
                renderCurrentPage();
            }
            logger.debug("Page mode set to {}", mode);
        }
    }

    /**
     * Renders the current page(s) based on the current page mode.
     */
    private void renderCurrentPage() {
        if (document == null) {
            return;
        }

        pagesPanel.removeAll();

        try {
            switch (pageMode) {
                case SINGLE_PAGE:
                    renderSinglePage();
                    break;
                case CONTINUOUS_SCROLL:
                    renderContinuousScroll();
                    break;
                case TWO_PAGES:
                    renderTwoPages();
                    break;
            }

            pagesPanel.revalidate();
            pagesPanel.repaint();

        } catch (PdfService.PdfProcessingException e) {
            logger.error("Error rendering page {}", currentPage, e);
            JLabel errorLabel = new JLabel("Error rendering page " + currentPage);
            errorLabel.setForeground(Color.RED);
            pagesPanel.add(errorLabel);
            pagesPanel.revalidate();
            pagesPanel.repaint();
        }
    }

    /**
     * Renders a single page.
     */
    private void renderSinglePage() throws PdfService.PdfProcessingException {
        BufferedImage pageImage = pdfService.renderPage(
                document, currentPage, zoomManager.getZoomFactor());

        JLabel pageLabel = new JLabel(new ImageIcon(pageImage));
        pageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel pageContainer = new JPanel();
        pageContainer.setLayout(new BoxLayout(pageContainer, BoxLayout.Y_AXIS));
        pageContainer.setBackground(Color.WHITE);
        pageContainer.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        pageContainer.add(pageLabel);

        JPanel centeredContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centeredContainer.setBackground(Color.DARK_GRAY);
        centeredContainer.add(pageContainer);

        pagesPanel.add(Box.createVerticalStrut(20));
        pagesPanel.add(centeredContainer);
        pagesPanel.add(Box.createVerticalStrut(20));
    }

    /**
     * Renders pages in continuous scroll mode.
     */
    private void renderContinuousScroll() throws PdfService.PdfProcessingException {
        // In a real implementation, this would render multiple pages
        // For now, just render the current page like single page mode
        renderSinglePage();

        // Scroll to ensure the current page is visible
        SwingUtilities.invokeLater(() -> {
            // This is a simplified approach; a real implementation would be more sophisticated
            scrollPane.getViewport().setViewPosition(new Point(0, 0));
        });
    }

    /**
     * Renders two pages side by side.
     */
    private void renderTwoPages() throws PdfService.PdfProcessingException {
        // Determine which pages to show (current page and next page, or two facing pages)
        int leftPage = (currentPage % 2 == 0) ? currentPage - 1 : currentPage;
        int rightPage = leftPage + 1;

        if (leftPage >= 1 && leftPage <= document.getPageCount()) {
            BufferedImage leftImage = pdfService.renderPage(
                    document, leftPage, zoomManager.getZoomFactor());

            JLabel leftLabel = new JLabel(new ImageIcon(leftImage));

            JPanel leftContainer = new JPanel();
            leftContainer.setLayout(new BoxLayout(leftContainer, BoxLayout.Y_AXIS));
            leftContainer.setBackground(Color.WHITE);
            leftContainer.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            leftContainer.add(leftLabel);

            if (rightPage <= document.getPageCount()) {
                BufferedImage rightImage = pdfService.renderPage(
                        document, rightPage, zoomManager.getZoomFactor());

                JLabel rightLabel = new JLabel(new ImageIcon(rightImage));

                JPanel rightContainer = new JPanel();
                rightContainer.setLayout(new BoxLayout(rightContainer, BoxLayout.Y_AXIS));
                rightContainer.setBackground(Color.WHITE);
                rightContainer.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                rightContainer.add(rightLabel);

                JPanel pagesContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
                pagesContainer.setBackground(Color.DARK_GRAY);
                pagesContainer.add(leftContainer);
                pagesContainer.add(rightContainer);

                pagesPanel.add(Box.createVerticalStrut(20));
                pagesPanel.add(pagesContainer);
                pagesPanel.add(Box.createVerticalStrut(20));
            } else {
                // Only one page to show
                JPanel centeredContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
                centeredContainer.setBackground(Color.DARK_GRAY);
                centeredContainer.add(leftContainer);

                pagesPanel.add(Box.createVerticalStrut(20));
                pagesPanel.add(centeredContainer);
                pagesPanel.add(Box.createVerticalStrut(20));
            }
        }
    }

    /**
     * Updates the UI state based on the current document and page.
     */
    private void updateUIState() {
        boolean hasDocument = document != null;

        // Update navigation controls
        firstPageButton.setEnabled(hasDocument && currentPage > 1);
        prevPageButton.setEnabled(hasDocument && currentPage > 1);
        nextPageButton.setEnabled(hasDocument && currentPage < getPageCount());
        lastPageButton.setEnabled(hasDocument && currentPage < getPageCount());
        pageNumberField.setEnabled(hasDocument);

        // Update page info
        updatePageInfo();
    }

    /**
     * Updates the page information display.
     */
    private void updatePageInfo() {
        if (document != null) {
            pageNumberField.setText(String.valueOf(currentPage));
            pageInfoLabel.setText(" / " + document.getPageCount());
        } else {
            pageNumberField.setText("0");
            pageInfoLabel.setText(" / 0");
        }
    }

    /**
     * Gets the total number of pages in the current document.
     *
     * @return The total number of pages, or 0 if no document is loaded
     */
    private int getPageCount() {
        return document != null ? document.getPageCount() : 0;
    }

    /**
     * Gets the current page number.
     *
     * @return The current page number, or 1 if no document is loaded
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * Gets the first displayed page in the current mode.
     * In TWO_PAGES mode, this returns the left page.
     * In other modes, this returns the current page.
     *
     * @return The first displayed page number, or 1 if no document is loaded
     */
    public int getFirstDisplayedPage() {
        if (pageMode == PageMode.TWO_PAGES && document != null) {
            // In two pages mode, calculate the left page (first displayed page)
            return (currentPage % 2 == 0) ? currentPage - 1 : currentPage;
        }
        return currentPage;
    }

    /**
     * Gets the current zoom level as a percentage.
     *
     * @return The current zoom level as a percentage (e.g., 100 for 100%)
     */
    public double getZoomLevel() {
        return zoomManager.getZoomPercentage();
    }

    /**
     * Adds a property change listener.
     *
     * @param listener The property change listener to add
     */
    public void addPropertyChangeListener(java.beans.PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Adds a property change listener for a specific property.
     *
     * @param propertyName The name of the property to listen for
     * @param listener The property change listener to add
     */
    public void addPropertyChangeListener(String propertyName, java.beans.PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Removes a property change listener.
     *
     * @param listener The property change listener to remove
     */
    public void removePropertyChangeListener(java.beans.PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /**
     * Removes a property change listener for a specific property.
     *
     * @param propertyName The name of the property to stop listening for
     * @param listener The property change listener to remove
     */
    public void removePropertyChangeListener(String propertyName, java.beans.PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
    }

    /**
     * Page display modes.
     */
    public enum PageMode {
        SINGLE_PAGE,
        CONTINUOUS_SCROLL,
        TWO_PAGES
    }

    /**
     * Creates the context menu for bookmark operations.
     */
    private void createContextMenu() {
        contextMenu = new JPopupMenu();

        JMenuItem addBookmarkItem = new JMenuItem("Add Bookmark");
        addBookmarkItem.addActionListener(e -> {
            if (document != null && addBookmarkCallback != null) {
                // Use the first displayed page (in 2-up mode, this will be the left page)
                addBookmarkCallback.accept(getFirstDisplayedPage());
            }
        });

        JMenuItem deleteBookmarkItem = new JMenuItem("Delete Bookmark");
        deleteBookmarkItem.addActionListener(e -> {
            if (document != null && deleteBookmarkCallback != null) {
                // Use the first displayed page (in 2-up mode, this will be the left page)
                deleteBookmarkCallback.accept(getFirstDisplayedPage());
            }
        });

        contextMenu.add(addBookmarkItem);
        contextMenu.add(deleteBookmarkItem);
    }

    /**
     * Shows the context menu at the specified location.
     *
     * @param e The mouse event that triggered the context menu
     */
    private void showContextMenu(MouseEvent e) {
        if (document != null) {
            contextMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    /**
     * Sets the callback to be invoked when adding a bookmark.
     *
     * @param callback The callback function that accepts a page number
     */
    public void setAddBookmarkCallback(Consumer<Integer> callback) {
        this.addBookmarkCallback = callback;
    }

    /**
     * Sets the callback to be invoked when deleting a bookmark.
     *
     * @param callback The callback function that accepts a page number
     */
    public void setDeleteBookmarkCallback(Consumer<Integer> callback) {
        this.deleteBookmarkCallback = callback;
    }
}
