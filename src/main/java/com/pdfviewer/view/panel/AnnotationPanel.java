package com.pdfviewer.view.panel;

import com.pdfviewer.model.entity.Annotation;
import com.pdfviewer.model.entity.PdfDocument;
import com.pdfviewer.model.repository.AnnotationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Panel for displaying and managing annotations.
 */
public class AnnotationPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(AnnotationPanel.class);

    // UI components
    private final JTable annotationTable;
    private final AnnotationTableModel tableModel;
    private final JPopupMenu contextMenu;
    private final JButton addAnnotationButton;
    private final JComboBox<String> filterComboBox;

    // Document state
    private PdfDocument document;
    private int currentPage = 1;

    // Callback for page selection
    private Consumer<Integer> pageSelectionCallback;

    // Repository for annotation persistence
    private final AnnotationRepository annotationRepository;

    /**
     * Creates a new annotation panel.
     * 
     * @param annotationRepository The repository for annotation persistence
     */
    public AnnotationPanel(AnnotationRepository annotationRepository) {
        super(new BorderLayout());

        this.annotationRepository = annotationRepository;

        // Create the table model
        tableModel = new AnnotationTableModel();

        // Create the table
        annotationTable = new JTable(tableModel);
        annotationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        annotationTable.setRowHeight(25);
        annotationTable.setAutoCreateRowSorter(true);

        // Set column widths
        annotationTable.getColumnModel().getColumn(0).setPreferredWidth(150); // Type
        annotationTable.getColumnModel().getColumn(1).setPreferredWidth(250); // Content
        annotationTable.getColumnModel().getColumn(2).setPreferredWidth(50);  // Page
        annotationTable.getColumnModel().getColumn(3).setPreferredWidth(150); // Date

        // Set custom renderer for date column
        annotationTable.getColumnModel().getColumn(3).setCellRenderer(new DateCellRenderer());

        // Add double-click listener
        annotationTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    navigateToSelectedAnnotation();
                } else if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                    // Show context menu on right-click
                    int row = annotationTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        annotationTable.setRowSelectionInterval(row, row);
                        contextMenu.show(annotationTable, e.getX(), e.getY());
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = annotationTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        annotationTable.setRowSelectionInterval(row, row);
                        contextMenu.show(annotationTable, e.getX(), e.getY());
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = annotationTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        annotationTable.setRowSelectionInterval(row, row);
                        contextMenu.show(annotationTable, e.getX(), e.getY());
                    }
                }
            }
        });

        // Create the context menu
        contextMenu = createContextMenu();

        // Create the toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        // Add annotation button
        addAnnotationButton = new JButton("Add Annotation");
        addAnnotationButton.setEnabled(false);
        addAnnotationButton.addActionListener(e -> addAnnotation());
        toolBar.add(addAnnotationButton);

        toolBar.addSeparator();

        // Filter label
        JLabel filterLabel = new JLabel("Filter: ");
        toolBar.add(filterLabel);

        // Filter combo box
        filterComboBox = new JComboBox<>(new String[]{"All", "Highlight", "Note", "Link", "Current Page"});
        filterComboBox.addActionListener(e -> applyFilter());
        toolBar.add(filterComboBox);

        // Add components to the panel
        add(toolBar, BorderLayout.NORTH);
        add(new JScrollPane(annotationTable), BorderLayout.CENTER);

        // Set initial state
        updateUIState();
    }

    /**
     * Sets the document to display annotations for.
     *
     * @param document The PDF document
     */
    public void setDocument(PdfDocument document) {
        this.document = document;
        this.currentPage = 1;
        loadAnnotations();
        updateUIState();
    }

    /**
     * Clears the current document.
     */
    public void clearDocument() {
        this.document = null;
        this.currentPage = 1;
        tableModel.clearAnnotations();
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

            // If "Current Page" filter is selected, refresh the view
            if (filterComboBox.getSelectedIndex() == 4) {
                applyFilter();
            }

            updateUIState();
        }
    }

    /**
     * Sets the callback to be invoked when an annotation is selected.
     *
     * @param callback The callback function
     */
    public void setPageSelectionCallback(Consumer<Integer> callback) {
        this.pageSelectionCallback = callback;
    }

    /**
     * Creates the context menu for annotation operations.
     *
     * @return The context menu
     */
    private JPopupMenu createContextMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem addItem = new JMenuItem("Add Annotation");
        addItem.addActionListener(e -> addAnnotation());

        JMenuItem editItem = new JMenuItem("Edit Annotation");
        editItem.addActionListener(e -> editSelectedAnnotation());

        JMenuItem deleteItem = new JMenuItem("Delete Annotation");
        deleteItem.addActionListener(e -> deleteSelectedAnnotation());

        JMenuItem goToItem = new JMenuItem("Go to Page");
        goToItem.addActionListener(e -> navigateToSelectedAnnotation());

        menu.add(addItem);
        menu.add(editItem);
        menu.add(deleteItem);
        menu.addSeparator();
        menu.add(goToItem);

        return menu;
    }

    /**
     * Loads annotations from the repository for the current document.
     */
    private void loadAnnotations() {
        tableModel.clearAnnotations();

        if (document != null) {
            try {
                // Load annotations from the repository
                List<Annotation> annotations = annotationRepository.findAllByFileId(document.getFileName());

                // Add annotations to the table
                for (Annotation annotation : annotations) {
                    AnnotationInfo annotationInfo = new AnnotationInfo(
                            annotation.getId(),
                            annotation.getType().name(),
                            annotation.getContent(),
                            annotation.getPageNumber(),
                            annotation.getCreatedAt());
                    tableModel.addAnnotation(annotationInfo);
                }

                logger.info("Loaded {} annotations for document: {}", annotations.size(), document.getFileName());
            } catch (SQLException e) {
                logger.error("Failed to load annotations", e);
                JOptionPane.showMessageDialog(this,
                        "Failed to load annotations: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Updates the UI state based on the current document and page.
     */
    private void updateUIState() {
        boolean hasDocument = document != null;
        addAnnotationButton.setEnabled(hasDocument);

        // Log the current state for debugging
        logger.debug("UI state updated: document={}, button enabled={}", 
                     hasDocument ? document.getFileName() : "null", 
                     addAnnotationButton.isEnabled());
    }

    /**
     * Applies the selected filter to the annotation table.
     */
    private void applyFilter() {
        TableRowSorter<AnnotationTableModel> sorter = new TableRowSorter<>(tableModel);
        annotationTable.setRowSorter(sorter);

        String filterType = (String) filterComboBox.getSelectedItem();
        if (filterType == null || filterType.equals("All")) {
            sorter.setRowFilter(null);
        } else if (filterType.equals("Current Page")) {
            sorter.setRowFilter(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, currentPage, 2));
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("^" + filterType + "$", 0));
        }
    }

    /**
     * Adds an annotation for the current page.
     */
    private void addAnnotation() {
        if (document == null) {
            return;
        }

        // Create a panel for the annotation input
        JPanel panel = new JPanel(new GridLayout(0, 1));

        JComboBox<String> typeComboBox = new JComboBox<>(new String[]{"HIGHLIGHT", "NOTE", "UNDERLINE", "STRIKETHROUGH", "DRAWING", "TEXT"});
        JTextArea contentArea = new JTextArea(5, 20);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);

        // Add color picker
        JPanel colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel colorLabel = new JLabel("Color: ");
        JButton colorButton = new JButton("Choose Color");
        final Color[] selectedColor = {Color.YELLOW}; // Default color
        colorButton.setBackground(selectedColor[0]);
        colorButton.addActionListener(e -> {
            Color color = JColorChooser.showDialog(this, "Choose Annotation Color", selectedColor[0]);
            if (color != null) {
                selectedColor[0] = color;
                colorButton.setBackground(color);
            }
        });
        colorPanel.add(colorLabel);
        colorPanel.add(colorButton);

        panel.add(new JLabel("Type:"));
        panel.add(typeComboBox);
        panel.add(new JLabel("Content:"));
        panel.add(new JScrollPane(contentArea));
        panel.add(colorPanel);

        // Show the dialog
        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Add Annotation",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String type = (String) typeComboBox.getSelectedItem();
            String content = contentArea.getText().trim();

            if (content.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Annotation content cannot be empty.",
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Convert color to hex string
                String colorHex = String.format("#%02x%02x%02x", 
                        selectedColor[0].getRed(), 
                        selectedColor[0].getGreen(), 
                        selectedColor[0].getBlue());

                // Create and save the annotation to the repository
                Annotation annotation = new Annotation.Builder()
                        .fileId(document.getFileName())
                        .pageNumber(currentPage)
                        .type(Annotation.AnnotationType.valueOf(type))
                        .content(content)
                        .x(0) // Default position, would be set by actual selection in a real implementation
                        .y(0)
                        .width(100f) // Default size
                        .height(20f)
                        .color(colorHex)
                        .build();

                annotationRepository.save(annotation);

                // Create the annotation info for the UI
                AnnotationInfo annotationInfo = new AnnotationInfo(
                        annotation.getId(),
                        type,
                        content,
                        currentPage,
                        annotation.getCreatedAt());

                // Add to the table
                tableModel.addAnnotation(annotationInfo);

                // Apply filter (in case "Current Page" is selected)
                applyFilter();

                logger.info("Added {} annotation on page {}", type, currentPage);
            } catch (SQLException e) {
                logger.error("Failed to save annotation", e);
                JOptionPane.showMessageDialog(this,
                        "Failed to save annotation: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid annotation type", e);
                JOptionPane.showMessageDialog(this,
                        "Invalid annotation type: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Edits the selected annotation.
     */
    private void editSelectedAnnotation() {
        int selectedRow = annotationTable.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }

        // Convert view row index to model row index
        int modelRow = annotationTable.convertRowIndexToModel(selectedRow);
        AnnotationInfo annotationInfo = tableModel.getAnnotationAt(modelRow);

        try {
            // Get the original annotation from the repository
            annotationRepository.findById(annotationInfo.getId()).ifPresent(originalAnnotation -> {
                // Create a panel for the annotation input
                JPanel panel = new JPanel(new GridLayout(0, 1));

                JComboBox<String> typeComboBox = new JComboBox<>(new String[]{"HIGHLIGHT", "NOTE", "UNDERLINE", "STRIKETHROUGH", "DRAWING", "TEXT"});
                typeComboBox.setSelectedItem(annotationInfo.getType());

                JTextArea contentArea = new JTextArea(5, 20);
                contentArea.setText(annotationInfo.getContent());
                contentArea.setLineWrap(true);
                contentArea.setWrapStyleWord(true);

                // Add color picker
                JPanel colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                JLabel colorLabel = new JLabel("Color: ");
                JButton colorButton = new JButton("Choose Color");

                // Parse the original color
                Color originalColor = Color.YELLOW; // Default
                try {
                    String colorHex = originalAnnotation.getColor();
                    if (colorHex != null && colorHex.startsWith("#") && colorHex.length() == 7) {
                        int r = Integer.parseInt(colorHex.substring(1, 3), 16);
                        int g = Integer.parseInt(colorHex.substring(3, 5), 16);
                        int b = Integer.parseInt(colorHex.substring(5, 7), 16);
                        originalColor = new Color(r, g, b);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to parse annotation color: {}", originalAnnotation.getColor());
                }

                final Color[] selectedColor = {originalColor};
                colorButton.setBackground(selectedColor[0]);
                colorButton.addActionListener(e -> {
                    Color color = JColorChooser.showDialog(this, "Choose Annotation Color", selectedColor[0]);
                    if (color != null) {
                        selectedColor[0] = color;
                        colorButton.setBackground(color);
                    }
                });
                colorPanel.add(colorLabel);
                colorPanel.add(colorButton);

                panel.add(new JLabel("Type:"));
                panel.add(typeComboBox);
                panel.add(new JLabel("Content:"));
                panel.add(new JScrollPane(contentArea));
                panel.add(colorPanel);

                // Show the dialog
                int result = JOptionPane.showConfirmDialog(
                        this,
                        panel,
                        "Edit Annotation",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE);

                if (result == JOptionPane.OK_OPTION) {
                    String type = (String) typeComboBox.getSelectedItem();
                    String content = contentArea.getText().trim();

                    if (content.isEmpty()) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Annotation content cannot be empty.",
                                "Invalid Input",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    try {
                        // Convert color to hex string
                        String colorHex = String.format("#%02x%02x%02x", 
                                selectedColor[0].getRed(), 
                                selectedColor[0].getGreen(), 
                                selectedColor[0].getBlue());

                        // Update the annotation in the repository
                        Annotation updatedAnnotation = new Annotation.Builder()
                                .id(annotationInfo.getId())
                                .fileId(originalAnnotation.getFileId())
                                .pageNumber(originalAnnotation.getPageNumber())
                                .type(Annotation.AnnotationType.valueOf(type))
                                .content(content)
                                .x(originalAnnotation.getX())
                                .y(originalAnnotation.getY())
                                .width(originalAnnotation.getWidth())
                                .height(originalAnnotation.getHeight())
                                .color(colorHex)
                                .createdAt(originalAnnotation.getCreatedAt())
                                .build();

                        annotationRepository.save(updatedAnnotation);

                        // Update the annotation info in the UI
                        annotationInfo.setType(type);
                        annotationInfo.setContent(content);

                        // Update the table
                        tableModel.fireTableRowsUpdated(modelRow, modelRow);

                        logger.info("Edited annotation on page {}", annotationInfo.getPageNumber());
                    } catch (SQLException e) {
                        logger.error("Failed to update annotation", e);
                        JOptionPane.showMessageDialog(this,
                                "Failed to update annotation: " + e.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    } catch (IllegalArgumentException e) {
                        logger.error("Invalid annotation type", e);
                        JOptionPane.showMessageDialog(this,
                                "Invalid annotation type: " + e.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
        } catch (SQLException e) {
            logger.error("Failed to retrieve annotation for editing", e);
            JOptionPane.showMessageDialog(this,
                    "Failed to retrieve annotation: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Deletes the selected annotation.
     */
    private void deleteSelectedAnnotation() {
        int selectedRow = annotationTable.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }

        // Convert view row index to model row index
        int modelRow = annotationTable.convertRowIndexToModel(selectedRow);
        AnnotationInfo annotationInfo = tableModel.getAnnotationAt(modelRow);

        // Confirm deletion
        int result = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete this annotation?",
                "Delete Annotation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            try {
                // Delete the annotation from the repository
                annotationRepository.deleteById(annotationInfo.getId());

                // Remove the annotation from the table
                tableModel.removeAnnotation(modelRow);

                logger.info("Deleted annotation on page {}", annotationInfo.getPageNumber());
            } catch (SQLException e) {
                logger.error("Failed to delete annotation", e);
                JOptionPane.showMessageDialog(this,
                        "Failed to delete annotation: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Navigates to the page of the selected annotation.
     */
    private void navigateToSelectedAnnotation() {
        int selectedRow = annotationTable.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }

        // Convert view row index to model row index
        int modelRow = annotationTable.convertRowIndexToModel(selectedRow);
        AnnotationInfo annotation = tableModel.getAnnotationAt(modelRow);

        if (pageSelectionCallback != null) {
            pageSelectionCallback.accept(annotation.getPageNumber());
        }
    }

    /**
     * Table model for annotations.
     */
    private static class AnnotationTableModel extends AbstractTableModel {
        private static final String[] COLUMN_NAMES = {"Type", "Content", "Page", "Date"};
        private final List<AnnotationInfo> annotations = new ArrayList<>();

        @Override
        public int getRowCount() {
            return annotations.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            AnnotationInfo annotation = annotations.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> annotation.getType();
                case 1 -> annotation.getContent();
                case 2 -> annotation.getPageNumber();
                case 3 -> annotation.getCreatedAt();
                default -> null;
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0, 1 -> String.class;
                case 2 -> Integer.class;
                case 3 -> LocalDateTime.class;
                default -> Object.class;
            };
        }

        /**
         * Adds an annotation to the model.
         *
         * @param annotation The annotation to add
         */
        public void addAnnotation(AnnotationInfo annotation) {
            annotations.add(annotation);
            fireTableRowsInserted(annotations.size() - 1, annotations.size() - 1);
        }

        /**
         * Removes an annotation from the model.
         *
         * @param rowIndex The row index of the annotation to remove
         */
        public void removeAnnotation(int rowIndex) {
            if (rowIndex >= 0 && rowIndex < annotations.size()) {
                annotations.remove(rowIndex);
                fireTableRowsDeleted(rowIndex, rowIndex);
            }
        }

        /**
         * Gets the annotation at the specified row index.
         *
         * @param rowIndex The row index
         * @return The annotation at the specified row index
         */
        public AnnotationInfo getAnnotationAt(int rowIndex) {
            return annotations.get(rowIndex);
        }

        /**
         * Clears all annotations from the model.
         */
        public void clearAnnotations() {
            annotations.clear();
            fireTableDataChanged();
        }
    }

    /**
     * Custom cell renderer for date column.
     */
    private static class DateCellRenderer extends DefaultTableCellRenderer {
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                      boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value instanceof LocalDateTime) {
                setText(((LocalDateTime) value).format(DATE_FORMATTER));
            }

            return this;
        }
    }

    /**
     * Class representing annotation information.
     */
    private static class AnnotationInfo {
        private final String id;
        private String type;
        private String content;
        private final int pageNumber;
        private final LocalDateTime createdAt;

        /**
         * Creates a new annotation info.
         *
         * @param id The annotation ID
         * @param type The annotation type
         * @param content The annotation content
         * @param pageNumber The page number
         * @param createdAt The creation timestamp
         */
        public AnnotationInfo(String id, String type, String content, int pageNumber, LocalDateTime createdAt) {
            this.id = id;
            this.type = type;
            this.content = content;
            this.pageNumber = pageNumber;
            this.createdAt = createdAt;
        }

        /**
         * Gets the annotation ID.
         *
         * @return The annotation ID
         */
        public String getId() {
            return id;
        }

        /**
         * Gets the annotation type.
         *
         * @return The annotation type
         */
        public String getType() {
            return type;
        }

        /**
         * Sets the annotation type.
         *
         * @param type The annotation type
         */
        public void setType(String type) {
            this.type = type;
        }

        /**
         * Gets the annotation content.
         *
         * @return The annotation content
         */
        public String getContent() {
            return content;
        }

        /**
         * Sets the annotation content.
         *
         * @param content The annotation content
         */
        public void setContent(String content) {
            this.content = content;
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
    }
}
