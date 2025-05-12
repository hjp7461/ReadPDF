package com.pdfviewer.view.panel;

import com.pdfviewer.model.entity.PdfDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Panel for displaying the document outline (table of contents).
 */
public class OutlinePanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(OutlinePanel.class);
    
    // UI components
    private final JTree outlineTree;
    private final DefaultMutableTreeNode rootNode;
    private final DefaultTreeModel treeModel;
    
    // Document state
    private PdfDocument document;
    
    // Callback for page selection
    private Consumer<Integer> pageSelectionCallback;
    
    /**
     * Creates a new outline panel.
     */
    public OutlinePanel() {
        super(new BorderLayout());
        
        // Create the tree model
        rootNode = new DefaultMutableTreeNode("Document Outline");
        treeModel = new DefaultTreeModel(rootNode);
        
        // Create the tree
        outlineTree = new JTree(treeModel);
        outlineTree.setRootVisible(false);
        outlineTree.setShowsRootHandles(true);
        outlineTree.setCellRenderer(new OutlineTreeCellRenderer());
        
        // Add double-click listener
        outlineTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    navigateToSelectedOutlineItem();
                }
            }
        });
        
        // Add the tree to a scroll pane
        JScrollPane scrollPane = new JScrollPane(outlineTree);
        
        // Add components to the panel
        add(scrollPane, BorderLayout.CENTER);
        
        // Add a label at the bottom for instructions
        JLabel instructionLabel = new JLabel("Double-click an item to navigate to that page");
        instructionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        instructionLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(instructionLabel, BorderLayout.SOUTH);
    }
    
    /**
     * Sets the document to display the outline for.
     *
     * @param document The PDF document
     */
    public void setDocument(PdfDocument document) {
        this.document = document;
        loadOutline();
    }
    
    /**
     * Clears the current document.
     */
    public void clearDocument() {
        this.document = null;
        rootNode.removeAllChildren();
        treeModel.reload();
    }
    
    /**
     * Sets the callback to be invoked when an outline item is selected.
     *
     * @param callback The callback function
     */
    public void setPageSelectionCallback(Consumer<Integer> callback) {
        this.pageSelectionCallback = callback;
    }
    
    /**
     * Loads the outline from the current document.
     * In a real implementation, this would load the actual outline from the PDF.
     */
    private void loadOutline() {
        rootNode.removeAllChildren();
        
        if (document != null) {
            // In a real implementation, this would load the actual outline from the PDF
            // For now, we'll just create a sample outline
            
            // Create a sample outline structure
            DefaultMutableTreeNode chapter1 = new DefaultMutableTreeNode(
                    new OutlineItem("Chapter 1: Introduction", 1));
            
            DefaultMutableTreeNode section11 = new DefaultMutableTreeNode(
                    new OutlineItem("1.1 Overview", 2));
            DefaultMutableTreeNode section12 = new DefaultMutableTreeNode(
                    new OutlineItem("1.2 Background", 3));
            
            chapter1.add(section11);
            chapter1.add(section12);
            
            DefaultMutableTreeNode chapter2 = new DefaultMutableTreeNode(
                    new OutlineItem("Chapter 2: Methodology", 4));
            
            DefaultMutableTreeNode section21 = new DefaultMutableTreeNode(
                    new OutlineItem("2.1 Research Design", 5));
            DefaultMutableTreeNode section22 = new DefaultMutableTreeNode(
                    new OutlineItem("2.2 Data Collection", 6));
            DefaultMutableTreeNode section23 = new DefaultMutableTreeNode(
                    new OutlineItem("2.3 Analysis Techniques", 7));
            
            chapter2.add(section21);
            chapter2.add(section22);
            chapter2.add(section23);
            
            DefaultMutableTreeNode chapter3 = new DefaultMutableTreeNode(
                    new OutlineItem("Chapter 3: Results", 8));
            
            // Add more chapters if the document has enough pages
            if (document.getPageCount() > 10) {
                DefaultMutableTreeNode chapter4 = new DefaultMutableTreeNode(
                        new OutlineItem("Chapter 4: Discussion", 10));
                
                DefaultMutableTreeNode section41 = new DefaultMutableTreeNode(
                        new OutlineItem("4.1 Interpretation of Results", 11));
                DefaultMutableTreeNode section42 = new DefaultMutableTreeNode(
                        new OutlineItem("4.2 Implications", 12));
                
                chapter4.add(section41);
                chapter4.add(section42);
                
                DefaultMutableTreeNode chapter5 = new DefaultMutableTreeNode(
                        new OutlineItem("Chapter 5: Conclusion", 13));
                
                // Add chapters to the root
                rootNode.add(chapter1);
                rootNode.add(chapter2);
                rootNode.add(chapter3);
                rootNode.add(chapter4);
                rootNode.add(chapter5);
            } else {
                // For shorter documents, just add the first few chapters
                rootNode.add(chapter1);
                rootNode.add(chapter2);
                rootNode.add(chapter3);
            }
            
            // Reload the tree model
            treeModel.reload();
            
            // Expand all nodes
            for (int i = 0; i < outlineTree.getRowCount(); i++) {
                outlineTree.expandRow(i);
            }
            
            logger.info("Loaded outline for document: {}", document.getFileName());
        }
    }
    
    /**
     * Navigates to the page of the selected outline item.
     */
    private void navigateToSelectedOutlineItem() {
        TreePath selectionPath = outlineTree.getSelectionPath();
        if (selectionPath == null) {
            return;
        }
        
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
        if (node.getUserObject() instanceof OutlineItem) {
            OutlineItem item = (OutlineItem) node.getUserObject();
            
            if (pageSelectionCallback != null) {
                pageSelectionCallback.accept(item.getPageNumber());
                logger.debug("Navigated to outline item: {} (page {})", 
                        item.getTitle(), item.getPageNumber());
            }
        }
    }
    
    /**
     * Custom cell renderer for outline tree nodes.
     */
    private static class OutlineTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                     boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Component comp = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            
            if (value instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object userObject = node.getUserObject();
                
                if (userObject instanceof OutlineItem) {
                    OutlineItem item = (OutlineItem) userObject;
                    setText(item.getTitle() + " (Page " + item.getPageNumber() + ")");
                    
                    // Use different icons based on the level in the tree
                    int level = node.getLevel();
                    if (level == 1) {
                        // Chapter level
                        setIcon(UIManager.getIcon("Tree.closedIcon"));
                    } else {
                        // Section level
                        setIcon(UIManager.getIcon("Tree.leafIcon"));
                    }
                }
            }
            
            return comp;
        }
    }
    
    /**
     * Class representing an outline item.
     */
    private static class OutlineItem {
        private final String title;
        private final int pageNumber;
        
        /**
         * Creates a new outline item.
         *
         * @param title The title of the outline item
         * @param pageNumber The page number the item points to
         */
        public OutlineItem(String title, int pageNumber) {
            this.title = title;
            this.pageNumber = pageNumber;
        }
        
        /**
         * Gets the title of the outline item.
         *
         * @return The title
         */
        public String getTitle() {
            return title;
        }
        
        /**
         * Gets the page number the item points to.
         *
         * @return The page number
         */
        public int getPageNumber() {
            return pageNumber;
        }
        
        @Override
        public String toString() {
            return title + " (Page " + pageNumber + ")";
        }
    }
}