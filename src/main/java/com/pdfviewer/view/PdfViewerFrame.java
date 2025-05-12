package com.pdfviewer.view;

import com.pdfviewer.model.entity.Bookmark;
import com.pdfviewer.model.entity.PdfDocument;
import com.pdfviewer.model.repository.AnnotationRepository;
import com.pdfviewer.model.repository.BookmarkRepository;
import com.pdfviewer.model.repository.RecentFileRepository;
import com.pdfviewer.model.repository.db.SqliteBookmarkRepository;
import com.pdfviewer.model.service.PdfService;
import com.pdfviewer.view.panel.PdfRenderingPanel;
import com.pdfviewer.view.panel.StatusBarPanel;
import com.pdfviewer.view.panel.ThumbnailPanel;
import com.pdfviewer.view.panel.BookmarkPanel;
import com.pdfviewer.view.panel.AnnotationPanel;
import com.pdfviewer.view.panel.OutlinePanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Main application frame for the PDF Viewer.
 * This class implements the main UI framework including menu bar, toolbar, and status bar.
 */
public class PdfViewerFrame extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(PdfViewerFrame.class);

    // Services
    private final PdfService pdfService;
    private final BookmarkRepository bookmarkRepository;
    private final AnnotationRepository annotationRepository;
    private final RecentFileRepository recentFileRepository;

    // UI Components
    private final JMenuBar menuBar;
    private final JToolBar toolBar;
    private final StatusBarPanel statusBar;
    private final JTabbedPane sidebarPane;
    private final JSplitPane mainSplitPane;
    private final PdfRenderingPanel pdfRenderingPanel;
    private BookmarkPanel bookmarkPanel;

    // Document state
    private PdfDocument currentDocument;

    /**
     * Creates a new PDF Viewer frame.
     *
     * @param pdfService The PDF service to use for document operations
     */
    public PdfViewerFrame(PdfService pdfService) {
        this(pdfService, null, null, null);
    }

    /**
     * Creates a new PDF Viewer frame.
     *
     * @param pdfService The PDF service to use for document operations
     * @param bookmarkRepository The repository for bookmark persistence
     * @param annotationRepository The repository for annotation persistence
     * @param recentFileRepository The repository for recent file persistence
     */
    public PdfViewerFrame(PdfService pdfService, BookmarkRepository bookmarkRepository, AnnotationRepository annotationRepository, RecentFileRepository recentFileRepository) {
        super("PDF Viewer");
        this.pdfService = pdfService;
        this.bookmarkRepository = bookmarkRepository;
        this.annotationRepository = annotationRepository;
        this.recentFileRepository = recentFileRepository;

        // Set up the frame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // Create UI components
        menuBar = createMenuBar();
        toolBar = createToolBar();
        statusBar = new StatusBarPanel();
        sidebarPane = createSidebarPane();
        pdfRenderingPanel = new PdfRenderingPanel(pdfService);

        // Create the main split pane
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebarPane, pdfRenderingPanel);
        mainSplitPane.setDividerLocation(250);
        mainSplitPane.setOneTouchExpandable(true);

        // Set up the layout
        setJMenuBar(menuBar);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(toolBar, BorderLayout.NORTH);
        contentPanel.add(mainSplitPane, BorderLayout.CENTER);
        contentPanel.add(statusBar, BorderLayout.SOUTH);

        setContentPane(contentPanel);

        // Set up keyboard shortcuts
        setupKeyboardShortcuts();

        logger.info("PDF Viewer frame initialized");
    }

    /**
     * Creates the menu bar.
     *
     * @return The menu bar
     */
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem openMenuItem = new JMenuItem("Open...", KeyEvent.VK_O);
        openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        openMenuItem.addActionListener(e -> openFile());

        JMenuItem closeMenuItem = new JMenuItem("Close", KeyEvent.VK_C);
        closeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK));
        closeMenuItem.addActionListener(e -> closeDocument());

        JMenuItem exitMenuItem = new JMenuItem("Exit", KeyEvent.VK_X);
        exitMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        exitMenuItem.addActionListener(e -> exit());

        fileMenu.add(openMenuItem);
        fileMenu.add(closeMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);

        // View menu
        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);

        JMenuItem zoomInMenuItem = new JMenuItem("Zoom In", KeyEvent.VK_I);
        zoomInMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_DOWN_MASK));
        zoomInMenuItem.addActionListener(e -> pdfRenderingPanel.zoomIn());

        JMenuItem zoomOutMenuItem = new JMenuItem("Zoom Out", KeyEvent.VK_O);
        zoomOutMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK));
        zoomOutMenuItem.addActionListener(e -> pdfRenderingPanel.zoomOut());

        JMenuItem resetZoomMenuItem = new JMenuItem("Reset Zoom", KeyEvent.VK_R);
        resetZoomMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK));
        resetZoomMenuItem.addActionListener(e -> pdfRenderingPanel.resetZoom());

        JMenu pageModeMenu = new JMenu("Page Mode");
        pageModeMenu.setMnemonic(KeyEvent.VK_P);

        JRadioButtonMenuItem singlePageMenuItem = new JRadioButtonMenuItem("Single Page");
        singlePageMenuItem.setSelected(true);
        singlePageMenuItem.addActionListener(e -> pdfRenderingPanel.setPageMode(PdfRenderingPanel.PageMode.SINGLE_PAGE));

        JRadioButtonMenuItem continuousScrollMenuItem = new JRadioButtonMenuItem("Continuous Scroll");
        continuousScrollMenuItem.addActionListener(e -> pdfRenderingPanel.setPageMode(PdfRenderingPanel.PageMode.CONTINUOUS_SCROLL));

        JRadioButtonMenuItem twoPageMenuItem = new JRadioButtonMenuItem("Two Pages");
        twoPageMenuItem.addActionListener(e -> pdfRenderingPanel.setPageMode(PdfRenderingPanel.PageMode.TWO_PAGES));

        ButtonGroup pageModeGroup = new ButtonGroup();
        pageModeGroup.add(singlePageMenuItem);
        pageModeGroup.add(continuousScrollMenuItem);
        pageModeGroup.add(twoPageMenuItem);

        pageModeMenu.add(singlePageMenuItem);
        pageModeMenu.add(continuousScrollMenuItem);
        pageModeMenu.add(twoPageMenuItem);

        viewMenu.add(zoomInMenuItem);
        viewMenu.add(zoomOutMenuItem);
        viewMenu.add(resetZoomMenuItem);
        viewMenu.addSeparator();
        viewMenu.add(pageModeMenu);

        // Theme menu
        JMenu themeMenu = new JMenu("Theme");
        themeMenu.setMnemonic(KeyEvent.VK_T);

        JRadioButtonMenuItem lightThemeMenuItem = new JRadioButtonMenuItem("Light");
        lightThemeMenuItem.setSelected(true);
        lightThemeMenuItem.addActionListener(e -> setTheme("light"));

        JRadioButtonMenuItem darkThemeMenuItem = new JRadioButtonMenuItem("Dark");
        darkThemeMenuItem.addActionListener(e -> setTheme("dark"));

        JRadioButtonMenuItem sepiaThemeMenuItem = new JRadioButtonMenuItem("Sepia");
        sepiaThemeMenuItem.addActionListener(e -> setTheme("sepia"));

        JRadioButtonMenuItem highContrastThemeMenuItem = new JRadioButtonMenuItem("High Contrast");
        highContrastThemeMenuItem.addActionListener(e -> setTheme("high-contrast"));

        ButtonGroup themeGroup = new ButtonGroup();
        themeGroup.add(lightThemeMenuItem);
        themeGroup.add(darkThemeMenuItem);
        themeGroup.add(sepiaThemeMenuItem);
        themeGroup.add(highContrastThemeMenuItem);

        themeMenu.add(lightThemeMenuItem);
        themeMenu.add(darkThemeMenuItem);
        themeMenu.add(sepiaThemeMenuItem);
        themeMenu.add(highContrastThemeMenuItem);

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);

        JMenuItem aboutMenuItem = new JMenuItem("About", KeyEvent.VK_A);
        aboutMenuItem.addActionListener(e -> showAboutDialog());

        helpMenu.add(aboutMenuItem);

        // Bookmarks menu
        JMenu bookmarksMenu = new JMenu("Bookmarks");
        bookmarksMenu.setMnemonic(KeyEvent.VK_B);

        JMenuItem addBookmarkMenuItem = new JMenuItem("Add Bookmark for Current Page", KeyEvent.VK_A);
        addBookmarkMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK));
        addBookmarkMenuItem.addActionListener(e -> addBookmarkForCurrentPage());

        JMenuItem showBookmarksMenuItem = new JMenuItem("Show Bookmarks Panel", KeyEvent.VK_S);
        showBookmarksMenuItem.addActionListener(e -> showBookmarksPanel());

        bookmarksMenu.add(addBookmarkMenuItem);
        bookmarksMenu.add(showBookmarksMenuItem);

        // Add menus to menu bar
        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(bookmarksMenu);
        menuBar.add(themeMenu);
        menuBar.add(helpMenu);

        return menuBar;
    }

    /**
     * Creates the toolbar.
     *
     * @return The toolbar
     */
    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        // Open button
        JButton openButton = new JButton("Open");
        openButton.setToolTipText("Open PDF File");
        openButton.addActionListener(e -> openFile());

        // Navigation buttons
        JButton prevPageButton = new JButton("Previous");
        prevPageButton.setToolTipText("Previous Page");
        prevPageButton.addActionListener(e -> pdfRenderingPanel.previousPage());

        JButton nextPageButton = new JButton("Next");
        nextPageButton.setToolTipText("Next Page");
        nextPageButton.addActionListener(e -> pdfRenderingPanel.nextPage());

        // Zoom buttons
        JButton zoomInButton = new JButton("+");
        zoomInButton.setToolTipText("Zoom In");
        zoomInButton.addActionListener(e -> pdfRenderingPanel.zoomIn());

        JButton zoomOutButton = new JButton("-");
        zoomOutButton.setToolTipText("Zoom Out");
        zoomOutButton.addActionListener(e -> pdfRenderingPanel.zoomOut());

        JButton resetZoomButton = new JButton("100%");
        resetZoomButton.setToolTipText("Reset Zoom");
        resetZoomButton.addActionListener(e -> pdfRenderingPanel.resetZoom());

        // Add components to toolbar
        toolBar.add(openButton);
        toolBar.addSeparator();
        toolBar.add(prevPageButton);
        toolBar.add(nextPageButton);
        toolBar.addSeparator();
        toolBar.add(zoomOutButton);
        toolBar.add(resetZoomButton);
        toolBar.add(zoomInButton);

        return toolBar;
    }

    /**
     * Creates the sidebar tabbed pane.
     *
     * @return The sidebar tabbed pane
     */
    private AnnotationPanel annotationPanel;

    private JTabbedPane createSidebarPane() {
        JTabbedPane tabbedPane = new JTabbedPane();

        // Create sidebar panels
        ThumbnailPanel thumbnailPanel = new ThumbnailPanel(pdfService);
        this.bookmarkPanel = new BookmarkPanel(bookmarkRepository);
        // Set up callback to navigate to the selected page when a bookmark is clicked
        this.bookmarkPanel.setPageSelectionCallback(pageNumber -> {
            if (pdfRenderingPanel != null) {
                pdfRenderingPanel.goToPage(pageNumber);
                logger.info("Navigated to page {} from bookmark", pageNumber);
            }
        });
        this.annotationPanel = new AnnotationPanel(annotationRepository);
        // Set up callback to navigate to the selected page when an annotation is clicked
        this.annotationPanel.setPageSelectionCallback(pageNumber -> {
            if (pdfRenderingPanel != null) {
                pdfRenderingPanel.goToPage(pageNumber);
                logger.info("Navigated to page {} from annotation", pageNumber);
            }
        });
        OutlinePanel outlinePanel = new OutlinePanel();

        // Add panels to tabbed pane
        tabbedPane.addTab("Thumbnails", new JScrollPane(thumbnailPanel));
        tabbedPane.addTab("Bookmarks", new JScrollPane(bookmarkPanel));
        tabbedPane.addTab("Annotations", new JScrollPane(annotationPanel));
        tabbedPane.addTab("Outline", new JScrollPane(outlinePanel));

        return tabbedPane;
    }

    /**
     * Sets up keyboard shortcuts.
     */
    private void setupKeyboardShortcuts() {
        // Add keyboard shortcuts using input and action maps
        JRootPane rootPane = getRootPane();

        // Open file (Ctrl+O)
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK), "openFile");
        rootPane.getActionMap().put("openFile", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFile();
            }
        });

        // Close document (Ctrl+W)
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK), "closeDocument");
        rootPane.getActionMap().put("closeDocument", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeDocument();
            }
        });

        // Exit application (Ctrl+Q)
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK), "exit");
        rootPane.getActionMap().put("exit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exit();
            }
        });

        // Zoom in (Ctrl++)
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_DOWN_MASK), "zoomIn");
        rootPane.getActionMap().put("zoomIn", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pdfRenderingPanel.zoomIn();
            }
        });

        // Zoom out (Ctrl+-)
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK), "zoomOut");
        rootPane.getActionMap().put("zoomOut", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pdfRenderingPanel.zoomOut();
            }
        });

        // Reset zoom (Ctrl+0)
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK), "resetZoom");
        rootPane.getActionMap().put("resetZoom", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pdfRenderingPanel.resetZoom();
            }
        });

        // Next page (Right arrow or Page Down)
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "nextPage");
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), "nextPage");
        rootPane.getActionMap().put("nextPage", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pdfRenderingPanel.nextPage();
            }
        });

        // Previous page (Left arrow or Page Up)
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "previousPage");
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), "previousPage");
        rootPane.getActionMap().put("previousPage", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pdfRenderingPanel.previousPage();
            }
        });
    }

    /**
     * Opens a PDF file.
     */
    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".pdf");
            }

            @Override
            public String getDescription() {
                return "PDF Files (*.pdf)";
            }
        });

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            openDocument(selectedFile);
        }
    }

    /**
     * Opens a PDF document.
     *
     * @param file The PDF file to open
     */
    public void openDocument(File file) {
        try {
            // Close current document if open
            closeDocument();

            // Update status bar
            statusBar.setMessage("Opening " + file.getName() + "...");

            // Open the document
            currentDocument = pdfService.openDocument(file);

            // Update UI
            setTitle("PDF Viewer - " + currentDocument.getFileName());
            pdfRenderingPanel.setDocument(currentDocument);

            // Update bookmark panel with current document
            if (bookmarkPanel != null) {
                bookmarkPanel.setDocument(currentDocument);
                showBookmarksPanel();
            }

            // Update annotation panel with current document
            if (annotationPanel != null) {
                annotationPanel.setDocument(currentDocument);
            }

            // Restore last read position if available
            if (recentFileRepository != null) {
                try {
                    String absolutePath = file.getAbsolutePath();
                    logger.info("Looking for recent file with path: {}", absolutePath);

                    Optional<com.pdfviewer.model.entity.RecentFile> recentFileOpt = recentFileRepository.findByFilePath(absolutePath);

                    if (recentFileOpt.isPresent()) {
                        com.pdfviewer.model.entity.RecentFile recentFile = recentFileOpt.get();
                        int lastPageViewed = recentFile.getLastPageViewed();
                        float lastZoomLevel = recentFile.getLastZoomLevel();

                        logger.info("Found recent file: id={}, lastPageViewed={}, lastZoomLevel={}", 
                                    recentFile.getId(), lastPageViewed, lastZoomLevel);

                        // Only prompt if there's a valid last page
                        if (lastPageViewed > 0 && lastPageViewed <= currentDocument.getPageCount()) {
                            // Ask user if they want to navigate to the last read page
                            int response = JOptionPane.showConfirmDialog(
                                this,
                                "이 PDF 파일을 마지막으로 " + lastPageViewed + "페이지에서 읽었습니다. 마지막으로 읽은 페이지로 이동하시겠습니까?",
                                "마지막 읽은 페이지",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE
                            );

                            if (response == JOptionPane.YES_OPTION) {
                                // User chose to navigate to the last page
                                pdfRenderingPanel.goToPage(lastPageViewed);
                                statusBar.setMessage("마지막으로 읽은 " + lastPageViewed + "페이지로 이동했습니다");
                                logger.info("Navigated to last read page: {}", lastPageViewed);
                            } else {
                                // User chose to stay on the first page
                                pdfRenderingPanel.goToPage(1);
                                statusBar.setMessage("첫 페이지로 이동했습니다");
                                logger.info("Stayed on first page as per user choice");
                            }
                        } else {
                            logger.info("Last page viewed ({}) is invalid for document with {} pages", 
                                       lastPageViewed, currentDocument.getPageCount());
                        }

                        // Restore zoom level
                        // Note: This would require additional methods in PdfRenderingPanel
                        // to set a specific zoom level
                    } else {
                        logger.info("No recent file found for path: {}", absolutePath);
                    }
                } catch (SQLException e) {
                    logger.warn("Failed to restore reading position", e);
                }
            }

            // Update reading progress
            updateReadingProgress();

            // The current page for bookmark and annotation panels will be set by the property change listener

            // Add a property change listener to update reading progress and sidebar panels when page changes
            pdfRenderingPanel.addPropertyChangeListener("currentPage", evt -> {
                updateReadingProgress();

                int currentPage = pdfRenderingPanel.getCurrentPage();

                // Update the current page in the bookmark panel
                if (bookmarkPanel != null) {
                    bookmarkPanel.setCurrentPage(currentPage);
                }

                // Update the current page in the annotation panel
                if (annotationPanel != null) {
                    annotationPanel.setCurrentPage(currentPage);
                }
            });

            logger.info("Opened document: {}", currentDocument.getFileName());
        } catch (PdfService.PdfProcessingException e) {
            logger.error("Error opening document", e);
            JOptionPane.showMessageDialog(this,
                    "Error opening PDF document: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            statusBar.setMessage("Error opening document");
        }
    }

    /**
     * Closes the current document.
     */
    private void closeDocument() {
        if (currentDocument != null) {
            // Save reading position before closing
            if (recentFileRepository != null) {
                try {
                    int currentPage = pdfRenderingPanel.getCurrentPage();
                    double zoomLevel = pdfRenderingPanel.getZoomLevel();

                    // Check if this file is already in recent files
                    String filePath = currentDocument.getFilePath();
                    logger.info("Saving reading position for file: {}", filePath);
                    logger.info("Current page: {}, zoom level: {}", currentPage, zoomLevel);

                    Optional<com.pdfviewer.model.entity.RecentFile> recentFileOpt = recentFileRepository.findByFilePath(filePath);

                    if (recentFileOpt.isPresent()) {
                        // Update existing record
                        com.pdfviewer.model.entity.RecentFile recentFile = recentFileOpt.get();
                        logger.info("Updating existing recent file record: id={}", recentFile.getId());

                        try {
                            boolean updated = recentFileRepository.updateLastViewedInfo(
                                recentFile.getId(), 
                                currentPage, 
                                (float) zoomLevel
                            );

                            if (updated) {
                                logger.info("Successfully updated reading position: page {}, zoom {}", currentPage, zoomLevel);
                            } else {
                                logger.warn("Failed to update reading position: no rows affected");
                            }
                        } catch (SQLException e) {
                            logger.warn("Failed to update reading position", e);
                        }
                    } else {
                        // Create new record
                        logger.info("No existing record found, creating new recent file entry");

                        try {
                            com.pdfviewer.model.entity.RecentFile recentFile = new com.pdfviewer.model.entity.RecentFile.Builder()
                                .filePath(filePath)
                                .fileName(currentDocument.getFileName())
                                .pageCount(currentDocument.getPageCount())
                                .lastPageViewed(currentPage)
                                .lastZoomLevel((float) zoomLevel)
                                .build();

                            recentFile = recentFileRepository.save(recentFile);
                            logger.info("Successfully saved reading position: id={}, page {}, zoom {}", 
                                       recentFile.getId(), currentPage, zoomLevel);
                        } catch (SQLException e) {
                            logger.warn("Failed to save reading position", e);
                        }
                    }

                    // Update reading progress in status bar
                    updateReadingProgress();
                } catch (SQLException e) {
                    logger.warn("Failed to save reading position", e);
                }
            }

            // Reset bookmarks in the UI
            if (bookmarkPanel != null) {
                bookmarkPanel.clearDocument();
                logger.info("Bookmarks reset");
            }

            // Reset annotations in the UI
            if (annotationPanel != null) {
                annotationPanel.clearDocument();
                logger.info("Annotations reset");
            }

            pdfService.closeDocument(currentDocument);
            pdfRenderingPanel.clearDocument();
            setTitle("PDF Viewer");
            statusBar.setMessage("Document closed");
            currentDocument = null;
            logger.info("Document closed");
        }
    }

    /**
     * Exits the application.
     */
    private void exit() {
        closeDocument();
        dispose();
        System.exit(0);
    }

    /**
     * Shows the about dialog.
     */
    private void showAboutDialog() {
        JOptionPane.showMessageDialog(this,
                "PDF Viewer\nVersion 1.0\n\nA modern PDF viewer application.",
                "About PDF Viewer",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Sets the application theme.
     *
     * @param themeName The name of the theme to set
     */
    private void setTheme(String themeName) {
        // This would be implemented with a proper theme manager
        logger.info("Setting theme: {}", themeName);
        statusBar.setMessage("Theme changed to " + themeName);

        // For now, just log the theme change
        // In a real implementation, this would apply the theme to all components
    }

    /**
     * Updates the reading progress display in the status bar.
     */
    private void updateReadingProgress() {
        if (currentDocument != null) {
            int currentPage = pdfRenderingPanel.getCurrentPage();
            int totalPages = currentDocument.getPageCount();
            double percentage = (double) currentPage / totalPages * 100;

            statusBar.setMessage(String.format("Page %d of %d (%.1f%%)", 
                currentPage, totalPages, percentage));

            logger.debug("Reading progress updated: {}/{} pages ({}%)", 
                currentPage, totalPages, String.format("%.1f", percentage));
        }
    }

    /**
     * Adds a bookmark for the current page.
     * This method creates a dialog for the user to enter bookmark information.
     */
    private void addBookmarkForCurrentPage() {
        if (currentDocument == null) {
            return;
        }

        // Use the first displayed page (in 2-up mode, this will be the left page)
        int currentPage = pdfRenderingPanel.getFirstDisplayedPage();

        // Update the current page in the bookmark panel
        if (bookmarkPanel != null) {
            bookmarkPanel.setCurrentPage(currentPage);
        }

        // Create a panel for the bookmark input
        JPanel panel = new JPanel(new GridLayout(0, 1));

        JTextField nameField = new JTextField();
        JTextArea descriptionArea = new JTextArea(3, 20);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);

        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Description (optional):"));
        panel.add(new JScrollPane(descriptionArea));

        // Show the dialog
        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Add Bookmark",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String description = descriptionArea.getText().trim();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Bookmark name cannot be empty.",
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Create and save the bookmark to the repository
                if (bookmarkRepository != null) {
                    // First, ensure the file exists in the recent_files table
                    String fileId = ensureFileInRecentFiles();

                    Bookmark bookmark = new Bookmark.Builder()
                            .fileId(fileId)
                            .pageNumber(currentPage)
                            .name(name)
                            .description(description)
                            .build();

                    bookmarkRepository.save(bookmark);

                    // Reload bookmarks in the bookmark panel
                    if (bookmarkPanel != null) {
                        // Reload the bookmarks using the public reloadBookmarks() method
                        bookmarkPanel.reloadBookmarks();
                    }

                    // Show the bookmarks panel
                    showBookmarksPanel();

                    logger.info("Added bookmark: {} for page {}", name, currentPage);
                }
            } catch (SQLException e) {
                logger.error("Failed to save bookmark", e);
                JOptionPane.showMessageDialog(this,
                        "Failed to save bookmark: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Ensures that the current file exists in the recent_files table.
     * If it doesn't exist, creates a new entry.
     * 
     * @return the file ID to use for bookmarks
     * @throws SQLException if a database access error occurs
     */
    private String ensureFileInRecentFiles() throws SQLException {
        if (currentDocument == null) {
            throw new IllegalStateException("No document is currently open");
        }

        String filePath = currentDocument.getFilePath();
        String fileId = currentDocument.getFileName(); // Default to using the filename as ID

        logger.info("Ensuring file exists in recent_files table: {}", filePath);

        // If we have a recent file repository, try to find or create the file entry
        if (recentFileRepository != null) {
            try {
                // Check if this file is already in recent files
                logger.info("Checking if file exists in recent_files table");
                Optional<com.pdfviewer.model.entity.RecentFile> existingFile = 
                    recentFileRepository.findByFilePath(filePath);

                if (existingFile.isPresent()) {
                    // Use the existing file's ID
                    fileId = existingFile.get().getId();
                    logger.info("Found existing file in recent_files table with ID: {}", fileId);
                } else {
                    // Create a new entry in the recent_files table
                    logger.info("File not found in recent_files table, creating new entry");
                    int currentPage = pdfRenderingPanel.getCurrentPage();
                    double zoomLevel = pdfRenderingPanel.getZoomLevel();

                    com.pdfviewer.model.entity.RecentFile recentFile = new com.pdfviewer.model.entity.RecentFile.Builder()
                        .filePath(filePath)
                        .fileName(currentDocument.getFileName())
                        .pageCount(currentDocument.getPageCount())
                        .lastPageViewed(currentPage)
                        .lastZoomLevel((float) zoomLevel)
                        .build();

                    recentFile = recentFileRepository.save(recentFile);
                    fileId = recentFile.getId();

                    logger.info("Created new recent file entry with ID: {}", fileId);
                }
            } catch (SQLException e) {
                logger.warn("Failed to access recent files repository", e);
                // Continue with the file path as ID
            }
        } else {
            // If we don't have a recent file repository, create a UUID to use as the file ID
            // This is a workaround for the case where recentFileRepository is null
            fileId = UUID.randomUUID().toString();
            logger.info("RecentFileRepository is null, using UUID as file ID: {}", fileId);

            // Create a new connection manager to access the database
            try {
                // Create a new connection manager
                logger.info("Creating new connection manager to access database directly");
                com.pdfviewer.model.repository.db.SqliteConnectionManager connectionManager = 
                    new com.pdfviewer.model.repository.db.SqliteConnectionManager();

                // Insert directly into the recent_files table
                try (Connection conn = connectionManager.getConnection()) {
                    String sql = "INSERT INTO recent_files (id, file_path, file_name, last_opened, page_count, last_page_viewed, last_zoom_level) " +
                                 "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                                 "ON CONFLICT (id) DO NOTHING";

                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, fileId);
                        stmt.setString(2, filePath);
                        stmt.setString(3, currentDocument.getFileName());
                        stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                        stmt.setInt(5, currentDocument.getPageCount());
                        stmt.setInt(6, pdfRenderingPanel.getCurrentPage());
                        stmt.setFloat(7, (float) pdfRenderingPanel.getZoomLevel());

                        int rowsAffected = stmt.executeUpdate();
                        logger.info("Direct database insert: {} rows affected", rowsAffected);
                        logger.info("Created new recent file entry with ID: {}", fileId);
                    }
                }
            } catch (SQLException e) {
                logger.error("Failed to create recent file entry", e);
                throw e;
            }
        }

        return fileId;
    }

    /**
     * Shows the bookmarks panel by selecting the Bookmarks tab in the sidebar.
     */
    private void showBookmarksPanel() {
        if (sidebarPane != null) {
            // Find the index of the Bookmarks tab
            for (int i = 0; i < sidebarPane.getTabCount(); i++) {
                if ("Bookmarks".equals(sidebarPane.getTitleAt(i))) {
                    sidebarPane.setSelectedIndex(i);
                    break;
                }
            }
        }
    }
}
