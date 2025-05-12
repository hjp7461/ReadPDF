package com.pdfviewer.view.panel;

import com.pdfviewer.model.entity.Bookmark;
import com.pdfviewer.model.entity.PdfDocument;
import com.pdfviewer.model.repository.BookmarkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * Panel for displaying and managing bookmarks.
 */
public class BookmarkPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(BookmarkPanel.class);

    // UI components
    private final JTree bookmarkTree;
    private final DefaultMutableTreeNode rootNode;
    private final DefaultTreeModel treeModel;
    private final JPopupMenu contextMenu;
    private final JButton addBookmarkButton;

    // Document state
    private PdfDocument document;
    private int currentPage = 1;

    // Callback for page selection
    private Consumer<Integer> pageSelectionCallback;

    // Repository for bookmark persistence
    private final BookmarkRepository bookmarkRepository;

    /**
     * Creates a new bookmark panel.
     * 
     * @param bookmarkRepository The repository for bookmark persistence
     */
    public BookmarkPanel(BookmarkRepository bookmarkRepository) {
        super(new BorderLayout());

        this.bookmarkRepository = bookmarkRepository;

        // Create the tree model
        rootNode = new DefaultMutableTreeNode("Bookmarks");
        treeModel = new DefaultTreeModel(rootNode);

        // Create the tree
        bookmarkTree = new JTree(treeModel);
        bookmarkTree.setRootVisible(false);
        bookmarkTree.setShowsRootHandles(true);
        bookmarkTree.setCellRenderer(new BookmarkTreeCellRenderer());
        bookmarkTree.addMouseListener(new BookmarkMouseListener());

        // Create the context menu
        contextMenu = createContextMenu();

        // Create the toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        // Add bookmark button
        addBookmarkButton = new JButton("Add Bookmark");
        addBookmarkButton.setEnabled(false);
        addBookmarkButton.addActionListener(e -> addBookmark());
        toolBar.add(addBookmarkButton);

        // Add components to the panel
        add(toolBar, BorderLayout.NORTH);
        add(new JScrollPane(bookmarkTree), BorderLayout.CENTER);

        // Set initial state
        updateUIState();
    }

    /**
     * Sets the document to display bookmarks for.
     *
     * @param document The PDF document
     */
    public void setDocument(PdfDocument document) {
        this.document = document;
        loadBookmarks();
        updateUIState();
    }

    /**
     * Reloads bookmarks from the repository for the current document.
     * This method can be called to refresh the bookmark panel after bookmarks have been added or removed.
     */
    public void reloadBookmarks() {
        if (document != null) {
            loadBookmarks();
        }
    }

    /**
     * Clears the current document.
     */
    public void clearDocument() {
        this.document = null;
        this.currentPage = 1;
        rootNode.removeAllChildren();
        treeModel.reload();
        updateUIState();
    }

    /**
     * Sets the current page.
     *
     * @param pageNumber The current page number (1-based)
     */
    public void setCurrentPage(int pageNumber) {
        if (document != null && pageNumber >= 1 && pageNumber <= document.getPageCount()) {
            this.currentPage = pageNumber;
            updateUIState();
        }
    }

    /**
     * Sets the callback to be invoked when a bookmark is selected.
     *
     * @param callback The callback function
     */
    public void setPageSelectionCallback(Consumer<Integer> callback) {
        this.pageSelectionCallback = callback;
    }

    /**
     * Creates the context menu for bookmark operations.
     *
     * @return The context menu
     */
    private JPopupMenu createContextMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem addItem = new JMenuItem("Add Bookmark");
        addItem.addActionListener(e -> addBookmark());

        JMenuItem editItem = new JMenuItem("Edit Bookmark");
        editItem.addActionListener(e -> editSelectedBookmark());

        JMenuItem deleteItem = new JMenuItem("Delete Bookmark");
        deleteItem.addActionListener(e -> deleteSelectedBookmark());

        menu.add(addItem);
        menu.add(editItem);
        menu.add(deleteItem);

        return menu;
    }

    /**
     * Loads bookmarks from the repository for the current document.
     */
    private void loadBookmarks() {
        rootNode.removeAllChildren();

        if (document != null) {
            try {
                // Load bookmarks from the repository
                List<Bookmark> bookmarks = bookmarkRepository.findAllByFileId(document.getFileName());

                // Add bookmarks to the tree
                for (Bookmark bookmark : bookmarks) {
                    DefaultMutableTreeNode node = new DefaultMutableTreeNode(
                            new BookmarkInfo(bookmark.getId(), bookmark.getName(), 
                                    bookmark.getDescription(), bookmark.getPageNumber(), 
                                    bookmark.getCreatedAt()));
                    rootNode.add(node);
                }

                logger.info("Loaded {} bookmarks for document: {}", bookmarks.size(), document.getFileName());
            } catch (SQLException e) {
                logger.error("Failed to load bookmarks", e);
                JOptionPane.showMessageDialog(this,
                        "Failed to load bookmarks: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }

        treeModel.reload();

        // Expand all nodes
        for (int i = 0; i < bookmarkTree.getRowCount(); i++) {
            bookmarkTree.expandRow(i);
        }
    }

    /**
     * Updates the UI state based on the current document and page.
     */
    private void updateUIState() {
        boolean hasDocument = document != null;

        if (hasDocument) {
            try {
                // Check if there's already a bookmark for the current page
                List<Bookmark> bookmarksForCurrentPage = 
                    bookmarkRepository.findAllByFileIdAndPageNumber(document.getFileName(), currentPage);

                // Enable the button only if there's no bookmark for the current page
                addBookmarkButton.setEnabled(bookmarksForCurrentPage.isEmpty());
            } catch (SQLException e) {
                logger.error("Failed to check for existing bookmarks", e);
                // In case of error, default to enabled if document is loaded
                addBookmarkButton.setEnabled(true);
            }
        } else {
            addBookmarkButton.setEnabled(false);
        }
    }

    /**
     * Adds a bookmark for the current page.
     */
    private void addBookmark() {
        if (document == null) {
            return;
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
                Bookmark bookmark = new Bookmark.Builder()
                        .fileId(document.getFileName())
                        .pageNumber(currentPage)
                        .name(name)
                        .description(description)
                        .build();

                bookmarkRepository.save(bookmark);

                // Create the bookmark info for the UI
                BookmarkInfo bookmarkInfo = new BookmarkInfo(
                        bookmark.getId(), name, description, currentPage, bookmark.getCreatedAt());
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(bookmarkInfo);

                // Add to the tree
                rootNode.add(node);
                treeModel.reload();

                // Select the new node
                TreePath path = new TreePath(node.getPath());
                bookmarkTree.setSelectionPath(path);
                bookmarkTree.scrollPathToVisible(path);

                logger.info("Added bookmark: {} for page {}", name, currentPage);
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
     * Edits the selected bookmark.
     */
    private void editSelectedBookmark() {
        TreePath selectionPath = bookmarkTree.getSelectionPath();
        if (selectionPath == null) {
            return;
        }

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
        if (node.getUserObject() instanceof BookmarkInfo) {
            BookmarkInfo bookmarkInfo = (BookmarkInfo) node.getUserObject();

            // Create a panel for the bookmark input
            JPanel panel = new JPanel(new GridLayout(0, 1));

            JTextField nameField = new JTextField(bookmarkInfo.getName());
            JTextArea descriptionArea = new JTextArea(3, 20);
            descriptionArea.setText(bookmarkInfo.getDescription());
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
                    "Edit Bookmark",
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
                    // Update the bookmark in the repository
                    Bookmark bookmark = new Bookmark.Builder()
                            .id(bookmarkInfo.getId())
                            .fileId(document.getFileName())
                            .pageNumber(bookmarkInfo.getPageNumber())
                            .name(name)
                            .description(description)
                            .createdAt(bookmarkInfo.getCreatedAt())
                            .build();

                    bookmarkRepository.save(bookmark);

                    // Update the bookmark info in the UI
                    bookmarkInfo.setName(name);
                    bookmarkInfo.setDescription(description);
                    treeModel.nodeChanged(node);

                    logger.info("Edited bookmark: {} for page {}", name, bookmarkInfo.getPageNumber());
                } catch (SQLException e) {
                    logger.error("Failed to update bookmark", e);
                    JOptionPane.showMessageDialog(this,
                            "Failed to update bookmark: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    /**
     * Deletes the selected bookmark.
     */
    private void deleteSelectedBookmark() {
        TreePath selectionPath = bookmarkTree.getSelectionPath();
        if (selectionPath == null) {
            return;
        }

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
        if (node.getUserObject() instanceof BookmarkInfo) {
            BookmarkInfo bookmarkInfo = (BookmarkInfo) node.getUserObject();

            // Confirm deletion
            int result = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to delete the bookmark \"" + bookmarkInfo.getName() + "\"?",
                    "Delete Bookmark",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (result == JOptionPane.YES_OPTION) {
                try {
                    // Delete the bookmark from the repository
                    bookmarkRepository.deleteById(bookmarkInfo.getId());

                    // Remove the node from the tree
                    treeModel.removeNodeFromParent(node);

                    logger.info("Deleted bookmark: {} for page {}", bookmarkInfo.getName(), bookmarkInfo.getPageNumber());
                } catch (SQLException e) {
                    logger.error("Failed to delete bookmark", e);
                    JOptionPane.showMessageDialog(this,
                            "Failed to delete bookmark: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    /**
     * Mouse listener for the bookmark tree.
     */
    private class BookmarkMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            // Get the path for the clicked location
            TreePath path = bookmarkTree.getPathForLocation(e.getX(), e.getY());
            if (path == null) {
                return; // No node was clicked
            }

            // Select the node
            bookmarkTree.setSelectionPath(path);

            // Handle left-click (navigate to bookmark)
            if (e.getButton() == MouseEvent.BUTTON1) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node.getUserObject() instanceof BookmarkInfo) {
                    BookmarkInfo bookmark = (BookmarkInfo) node.getUserObject();
                    if (pageSelectionCallback != null) {
                        logger.info("Navigating to bookmark page: {}", bookmark.getPageNumber());
                        pageSelectionCallback.accept(bookmark.getPageNumber());
                    }
                }
            } 
            // Handle right-click (show context menu)
            else if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                contextMenu.show(bookmarkTree, e.getX(), e.getY());
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                TreePath path = bookmarkTree.getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    bookmarkTree.setSelectionPath(path);
                    contextMenu.show(bookmarkTree, e.getX(), e.getY());
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                TreePath path = bookmarkTree.getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    bookmarkTree.setSelectionPath(path);
                    contextMenu.show(bookmarkTree, e.getX(), e.getY());
                }
            }
        }
    }

    /**
     * Custom cell renderer for bookmark tree nodes.
     */
    private static class BookmarkTreeCellRenderer extends DefaultTreeCellRenderer {
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                     boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Component comp = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            if (value instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object userObject = node.getUserObject();

                if (userObject instanceof BookmarkInfo) {
                    BookmarkInfo bookmark = (BookmarkInfo) userObject;
                    setText(bookmark.getName() + " (Page " + bookmark.getPageNumber() + ")");

                    // Create tooltip with description if available
                    StringBuilder tooltip = new StringBuilder();
                    tooltip.append("Added: ").append(bookmark.getCreatedAt().format(DATE_FORMATTER));

                    if (bookmark.getDescription() != null && !bookmark.getDescription().isEmpty()) {
                        tooltip.append("<br>").append(bookmark.getDescription());
                    }

                    setToolTipText("<html>" + tooltip + "</html>");
                    setIcon(UIManager.getIcon("FileView.bookmarkIcon"));
                }
            }

            return comp;
        }
    }

    /**
     * Class representing bookmark information.
     */
    private static class BookmarkInfo {
        private final String id;
        private String name;
        private String description;
        private final int pageNumber;
        private final LocalDateTime createdAt;

        /**
         * Creates a new bookmark info.
         *
         * @param id The bookmark ID
         * @param name The bookmark name
         * @param description The bookmark description
         * @param pageNumber The page number
         * @param createdAt The creation timestamp
         */
        public BookmarkInfo(String id, String name, String description, int pageNumber, LocalDateTime createdAt) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.pageNumber = pageNumber;
            this.createdAt = createdAt;
        }

        /**
         * Gets the bookmark ID.
         *
         * @return The bookmark ID
         */
        public String getId() {
            return id;
        }

        /**
         * Gets the bookmark name.
         *
         * @return The bookmark name
         */
        public String getName() {
            return name + " (Page " + pageNumber + ")";
        }

        /**
         * Sets the bookmark name.
         *
         * @param name The bookmark name
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Gets the bookmark description.
         *
         * @return The bookmark description
         */
        public String getDescription() {
            return description;
        }

        /**
         * Sets the bookmark description.
         *
         * @param description The bookmark description
         */
        public void setDescription(String description) {
            this.description = description;
        }

        /**
         * Gets the page number.
         *
         * @return The page number
         */
        public int getPageNumber() {
            return pageNumber;
        }

        /**
         * Gets the creation timestamp.
         *
         * @return The creation timestamp
         */
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        @Override
        public String toString() {
            return name + " (Page " + pageNumber + ")";
        }
    }
}
