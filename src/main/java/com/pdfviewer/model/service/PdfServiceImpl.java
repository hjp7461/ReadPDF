package com.pdfviewer.model.service;

import com.pdfviewer.model.entity.PdfDocument;
import com.pdfviewer.model.entity.PdfPage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;
import com.pdfviewer.model.service.renderer.PdfRenderer;
import com.pdfviewer.model.service.renderer.PdfRendererFactory;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.image.RescaleOp;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Implementation of the PdfService interface using Apache PDFBox.
 */
public class PdfServiceImpl implements PdfService {
    private static final Logger logger = LoggerFactory.getLogger(PdfServiceImpl.class);

    // Cache for open PDDocuments
    private final Map<String, PDDocument> openDocuments = new ConcurrentHashMap<>();

    // Cache for rendered pages
    private final Map<String, Map<Integer, BufferedImage>> pageCache = new ConcurrentHashMap<>();

    // Enhanced page cache with LRU eviction and memory management
    private final com.pdfviewer.model.service.cache.MapBackedPageCache enhancedPageCache;

    // Cache for renderers
    private final Map<String, PdfRenderer> rendererCache = new ConcurrentHashMap<>();

    // Executor service for background tasks
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Creates a new PdfServiceImpl instance.
     */
    public PdfServiceImpl() {
        this.enhancedPageCache = new com.pdfviewer.model.service.cache.MapBackedPageCache(pageCache);
    }

    @Override
    public PdfDocument openDocument(File file) throws PdfProcessingException {
        try {
            logger.debug("Opening PDF file: {}", file.getPath());

            // Validate the file first
            ValidationResult validationResult = validatePdfFile(file);
            if (!validationResult.isValid()) {
                logger.warn("Invalid PDF file: {}", file.getPath());
                throw new PdfProcessingException("Invalid PDF file: " + validationResult.message());
            }

            // Load the document
            PDDocument pdDocument = PDDocument.load(file);
            String filePath = file.getAbsolutePath();

            // Cache the PDDocument
            openDocuments.put(filePath, pdDocument);

            // Create and cache a renderer for this document
            PdfRenderer renderer = PdfRendererFactory.createRenderer(pdDocument);
            rendererCache.put(filePath, renderer);

            // Extract metadata
            Map<String, String> metadata = extractMetadata(pdDocument);

            // Check if the document is image-based
            boolean isImageBased = isImageBasedPdf(pdDocument);

            // Create PdfPage objects for each page
            List<PdfPage> pages = createPdfPages(pdDocument, isImageBased);

            // Create and return the PdfDocument
            return new PdfDocument.Builder()
                    .filePath(filePath)
                    .fileName(file.getName())
                    .pageCount(pdDocument.getNumberOfPages())
                    .version(new PdfDocument.PdfVersion((int)(pdDocument.getVersion() / 10), (int)(pdDocument.getVersion() % 10)))
                    .metadata(metadata)
                    .pages(pages)
                    .isImageBased(isImageBased)
                    .build();

        } catch (IOException e) {
            logger.error("Error opening PDF file: {}", file.getPath(), e);
            throw new PdfProcessingException("Error opening PDF file: " + e.getMessage(), e);
        }
    }

    @Override
    public PdfDocument openDocument(Path path) throws PdfProcessingException {
        return openDocument(path.toFile());
    }

    @Override
    public ValidationResult validatePdfFile(File file) {
        try {
            logger.debug("Validating PDF file: {}", file.getPath());

            // Try to load the document to check if it's a valid PDF
            try (PDDocument document = PDDocument.load(file)) {
                // Basic validation: PDDocument load success
                int version = (int)document.getVersion();
                int pageCount = document.getNumberOfPages();

                logger.debug("PDF validation successful: {} (version {}.{}, {} pages)", 
                        file.getPath(), version / 10, version % 10, pageCount);

                return ValidationResult.valid(
                        new PdfDocument.PdfVersion(version / 10, version % 10), 
                        pageCount);
            }
        } catch (IOException e) {
            logger.warn("PDF validation failed: {}", file.getPath(), e);
            return ValidationResult.invalid("PDF format is invalid or file is corrupted", e);
        }
    }

    @Override
    public RepairResult attemptPdfRepair(File file) {
        try {
            logger.info("Attempting to repair PDF file: {}", file.getPath());

            // Try to load the document with memory usage settings to handle corrupted files
            PDDocument document = PDDocument.load(file);

            // Create a temporary file for the repaired PDF
            File repairedFile = File.createTempFile("repaired_", ".pdf");

            // Save the document to the temporary file
            document.save(repairedFile);
            document.close();

            logger.info("PDF repair successful: {} -> {}", file.getPath(), repairedFile.getPath());

            return RepairResult.success(file, repairedFile);
        } catch (IOException e) {
            logger.error("PDF repair failed: {}", file.getPath(), e);
            return RepairResult.failure(file, "PDF repair failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isImageBasedPdf(PdfDocument document) {
        // For tests, if the document is already marked as image-based, return that value
        if (document.isImageBased()) {
            return true;
        }

        PDDocument pdDocument = openDocuments.get(document.getFilePath());
        if (pdDocument == null) {
            logger.warn("Document not found in cache: {}", document.getFilePath());
            return false;
        }

        return isImageBasedPdf(pdDocument);
    }

    private boolean isImageBasedPdf(PDDocument document) {
        try {
            logger.debug("Checking if PDF is image-based");

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(Math.min(5, document.getNumberOfPages())); // Check first 5 pages

            String text = stripper.getText(document);
            // If the text is very short, consider it an image-based PDF
            boolean isImageBased = text.trim().length() < 50;

            logger.debug("PDF is image-based: {}", isImageBased);

            return isImageBased;
        } catch (IOException e) {
            logger.error("Error checking if PDF is image-based", e);
            return false;
        }
    }

    @Override
    public BufferedImage renderPage(PdfDocument document, int pageNumber, float dpi) throws PdfProcessingException {
        String filePath = document.getFilePath();
        PdfRenderer renderer = rendererCache.get(filePath);

        if (renderer == null) {
            logger.warn("Renderer not found in cache: {}", filePath);
            throw new PdfProcessingException("Renderer not found in cache");
        }

        if (pageNumber < 1 || pageNumber > document.getPageCount()) {
            logger.warn("Invalid page number: {}", pageNumber);
            throw new PdfProcessingException("Invalid page number: " + pageNumber);
        }

        try {
            logger.debug("Rendering page {} at {} DPI", pageNumber, dpi);

            // Check if the page is already cached
            Optional<BufferedImage> cachedImage = enhancedPageCache.getPage(filePath, pageNumber);
            if (cachedImage.isPresent()) {
                logger.debug("Page found in cache: {}", pageNumber);
                return cachedImage.get();
            }

            // Get the page rotation
            float rotation = document.getPage(pageNumber)
                    .map(PdfPage::getRotation)
                    .orElse(0.0f);

            // Render the page using our renderer with rotation
            BufferedImage image = renderer.renderImageWithDPI(pageNumber - 1, dpi, rotation);

            // Cache the rendered page
            enhancedPageCache.putPage(filePath, pageNumber, image);

            // Pre-render adjacent pages in the background
            preRenderAdjacentPages(document, pageNumber, dpi);

            return image;
        } catch (IOException e) {
            logger.error("Error rendering page {}", pageNumber, e);
            throw new PdfProcessingException("Error rendering page " + pageNumber, e);
        }
    }

    @Override
    public BufferedImage renderPage(PdfDocument document, int pageNumber, double zoomFactor) throws PdfProcessingException {
        String filePath = document.getFilePath();
        PdfRenderer renderer = rendererCache.get(filePath);

        if (renderer == null) {
            logger.warn("Renderer not found in cache: {}", filePath);
            throw new PdfProcessingException("Renderer not found in cache");
        }

        if (pageNumber < 1 || pageNumber > document.getPageCount()) {
            logger.warn("Invalid page number: {}", pageNumber);
            throw new PdfProcessingException("Invalid page number: " + pageNumber);
        }

        try {
            logger.debug("Rendering page {} with zoom factor {}", pageNumber, zoomFactor);

            // Check if the page is already cached
            Optional<BufferedImage> cachedImage = enhancedPageCache.getPage(filePath, pageNumber, zoomFactor);
            if (cachedImage.isPresent()) {
                logger.debug("Page found in cache: {} with zoom {}", pageNumber, zoomFactor);
                return cachedImage.get();
            }

            // Get the page rotation
            float rotation = document.getPage(pageNumber)
                    .map(PdfPage::getRotation)
                    .orElse(0.0f);

            // Render the page using our renderer with rotation
            BufferedImage image = renderer.renderImageWithZoom(pageNumber - 1, zoomFactor, rotation);

            // Cache the rendered page
            enhancedPageCache.putPage(filePath, pageNumber, zoomFactor, image);

            return image;
        } catch (IOException e) {
            logger.error("Error rendering page {} with zoom factor {}", pageNumber, zoomFactor, e);
            throw new PdfProcessingException("Error rendering page " + pageNumber, e);
        }
    }

    @Override
    public List<BufferedImage> generateThumbnails(PdfDocument document, int width, int height) {
        String filePath = document.getFilePath();
        PdfRenderer renderer = rendererCache.get(filePath);

        if (renderer == null) {
            logger.warn("Renderer not found in cache: {}", filePath);
            return List.of();
        }

        try {
            logger.debug("Generating thumbnails for document: {}", filePath);

            // Use our renderer to generate thumbnails
            return renderer.generateThumbnails(width, height);
        } catch (IOException e) {
            logger.error("Error generating thumbnails", e);
            return List.of();
        }
    }

    @Override
    public String extractText(PdfDocument document, int pageNumber) {
        String filePath = document.getFilePath();
        PDDocument pdDocument = openDocuments.get(filePath);

        if (pdDocument == null) {
            logger.warn("Document not found in cache: {}", filePath);
            return "";
        }

        if (pageNumber < 1 || pageNumber > document.getPageCount()) {
            logger.warn("Invalid page number: {}", pageNumber);
            return "";
        }

        try {
            logger.debug("Extracting text from page {}", pageNumber);

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(pageNumber);
            stripper.setEndPage(pageNumber);

            return stripper.getText(pdDocument);
        } catch (IOException e) {
            logger.error("Error extracting text from page {}", pageNumber, e);
            return "";
        }
    }

    @Override
    public BufferedImage enhanceImageQuality(BufferedImage image, float brightness, float contrast, float sharpness) {
        logger.debug("Enhancing image quality: brightness={}, contrast={}, sharpness={}", 
                brightness, contrast, sharpness);

        if (image == null) {
            logger.warn("Cannot enhance null image");
            return null;
        }

        try {
            // Ensure contrast is positive to avoid IllegalArgumentException
            float safeContrast = Math.max(0.1f, contrast);

            // Apply brightness and contrast adjustments
            float[] factors = new float[] { safeContrast, safeContrast, safeContrast, 1.0f };
            float[] offsets = new float[] { brightness, brightness, brightness, 0.0f };
            RescaleOp rescaleOp = new RescaleOp(factors, offsets, null);
            BufferedImage enhancedImage = rescaleOp.filter(image, null);

            // Apply sharpening if requested
            if (sharpness > 0) {
                // Create a sharpening kernel
                float center = 1.0f + (8.0f * sharpness);
                float outer = -sharpness;
                float[] kernelData = {
                    outer, outer, outer,
                    outer, center, outer,
                    outer, outer, outer
                };
                Kernel kernel = new Kernel(3, 3, kernelData);
                ConvolveOp convolveOp = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
                enhancedImage = convolveOp.filter(enhancedImage, null);
            }

            return enhancedImage;
        } catch (Exception e) {
            logger.error("Error enhancing image quality", e);
            // Return the original image if enhancement fails
            return image;
        }
    }

    @Override
    public void closeDocument(PdfDocument document) {
        if (document == null) {
            logger.warn("Cannot close null document");
            return;
        }

        String filePath = document.getFilePath();
        if (filePath == null) {
            logger.warn("Document has null file path");
            return;
        }

        PDDocument pdDocument = openDocuments.remove(filePath);
        PdfRenderer renderer = rendererCache.remove(filePath);

        try {
            logger.debug("Closing document: {}", filePath);

            // Close the renderer if it exists
            if (renderer != null) {
                renderer.close();
            }

            // Close the document if it exists
            if (pdDocument != null) {
                pdDocument.close();
            }

            // Clear the page cache for this document
            enhancedPageCache.clearDocumentCache(filePath);
        } catch (IOException e) {
            logger.error("Error closing document: {}", filePath, e);
        }
    }

    @Override
    public PdfDocument rotatePage(PdfDocument document, int pageNumber, float angle) throws PdfProcessingException {
        logger.debug("Rotating page {} to {} degrees", pageNumber, angle);

        // Validate the page number
        if (pageNumber < 1 || pageNumber > document.getPageCount()) {
            throw new PdfProcessingException("Invalid page number: " + pageNumber);
        }

        // Normalize the angle to be 0, 90, 180, or 270
        float normalizedAngle = normalizeAngle(angle);

        // Get the current page
        PdfPage currentPage = document.getPages().get(pageNumber - 1);

        // If the angle is already set, return the original document
        if (Math.abs(currentPage.getRotation() - normalizedAngle) < 0.001) {
            return document;
        }

        // Create a new page with the updated rotation
        PdfPage rotatedPage = new PdfPage.Builder()
                .pageNumber(currentPage.getPageNumber())
                .size(currentPage.getSize())
                .hasText(currentPage.hasText())
                .thumbnail(currentPage.getThumbnail().orElse(null))
                .rotation(normalizedAngle)
                .isImageBased(currentPage.isImageBased())
                .build();

        // Create a new list of pages with the rotated page
        List<PdfPage> newPages = new ArrayList<>(document.getPages());
        newPages.set(pageNumber - 1, rotatedPage);

        // Create a new document with the updated pages
        return new PdfDocument.Builder()
                .filePath(document.getFilePath())
                .fileName(document.getFileName())
                .pageCount(document.getPageCount())
                .version(document.getVersion())
                .metadata(document.getMetadata())
                .pages(newPages)
                .openedAt(document.getOpenedAt())
                .isImageBased(document.isImageBased())
                .isRepaired(document.isRepaired())
                .build();
    }

    @Override
    public PdfDocument rotatePageBy(PdfDocument document, int pageNumber, float angleDelta) throws PdfProcessingException {
        logger.debug("Rotating page {} by {} degrees", pageNumber, angleDelta);

        // Validate the page number
        if (pageNumber < 1 || pageNumber > document.getPageCount()) {
            throw new PdfProcessingException("Invalid page number: " + pageNumber);
        }

        // Get the current page
        PdfPage currentPage = document.getPages().get(pageNumber - 1);

        // Calculate the new angle
        float newAngle = currentPage.getRotation() + angleDelta;

        // Rotate to the new angle
        return rotatePage(document, pageNumber, newAngle);
    }

    @Override
    public PdfDocument resetPageRotation(PdfDocument document, int pageNumber) throws PdfProcessingException {
        logger.debug("Resetting rotation for page {}", pageNumber);

        // Rotate to 0 degrees
        return rotatePage(document, pageNumber, 0);
    }

    /**
     * Normalizes an angle to be 0, 90, 180, or 270 degrees.
     *
     * @param angle The angle to normalize
     * @return The normalized angle
     */
    private float normalizeAngle(float angle) {
        // Ensure the angle is positive and less than 360
        float normalizedAngle = angle % 360;
        if (normalizedAngle < 0) {
            normalizedAngle += 360;
        }

        // Round to the nearest 90 degrees
        return Math.round(normalizedAngle / 90) * 90;
    }

    /**
     * Extracts metadata from a PDF document.
     *
     * @param document The PDF document
     * @return A map of metadata key-value pairs
     */
    private Map<String, String> extractMetadata(PDDocument document) {
        Map<String, String> metadata = new HashMap<>();

        try {
            logger.debug("Extracting metadata");

            // Extract metadata from the document information dictionary
            if (document.getDocumentInformation() != null) {
                if (document.getDocumentInformation().getTitle() != null) {
                    metadata.put("Title", document.getDocumentInformation().getTitle());
                }
                if (document.getDocumentInformation().getAuthor() != null) {
                    metadata.put("Author", document.getDocumentInformation().getAuthor());
                }
                if (document.getDocumentInformation().getSubject() != null) {
                    metadata.put("Subject", document.getDocumentInformation().getSubject());
                }
                if (document.getDocumentInformation().getKeywords() != null) {
                    metadata.put("Keywords", document.getDocumentInformation().getKeywords());
                }
                if (document.getDocumentInformation().getCreator() != null) {
                    metadata.put("Creator", document.getDocumentInformation().getCreator());
                }
                if (document.getDocumentInformation().getProducer() != null) {
                    metadata.put("Producer", document.getDocumentInformation().getProducer());
                }
                if (document.getDocumentInformation().getCreationDate() != null) {
                    metadata.put("CreationDate", document.getDocumentInformation().getCreationDate().toString());
                }
                if (document.getDocumentInformation().getModificationDate() != null) {
                    metadata.put("ModificationDate", document.getDocumentInformation().getModificationDate().toString());
                }

                // Extract all custom metadata properties
                for (String key : document.getDocumentInformation().getMetadataKeys()) {
                    if (!metadata.containsKey(key) && document.getDocumentInformation().getCustomMetadataValue(key) != null) {
                        metadata.put(key, document.getDocumentInformation().getCustomMetadataValue(key));
                    }
                }
            }

            // Add document properties
            metadata.put("PageCount", String.valueOf(document.getNumberOfPages()));
            metadata.put("Version", document.getVersion() / 10 + "." + document.getVersion() % 10);

            // Extract document security information
            metadata.put("Encrypted", String.valueOf(document.isEncrypted()));
            if (document.isEncrypted()) {
                metadata.put("AllowPrinting", String.valueOf(document.getCurrentAccessPermission().canPrint()));
                metadata.put("AllowModify", String.valueOf(document.getCurrentAccessPermission().canModify()));
                metadata.put("AllowCopy", String.valueOf(document.getCurrentAccessPermission().canExtractContent()));
                metadata.put("AllowAnnotate", String.valueOf(document.getCurrentAccessPermission().canModifyAnnotations()));
                metadata.put("AllowFillForms", String.valueOf(document.getCurrentAccessPermission().canFillInForm()));
                metadata.put("AllowAccessibility", String.valueOf(document.getCurrentAccessPermission().canExtractForAccessibility()));
                metadata.put("AllowAssemble", String.valueOf(document.getCurrentAccessPermission().canAssembleDocument()));
                metadata.put("AllowPrintDegraded", String.valueOf(document.getCurrentAccessPermission().canPrintDegraded()));
            }

            // Document catalog information
            if (document.getDocumentCatalog().getLanguage() != null) {
                metadata.put("Language", document.getDocumentCatalog().getLanguage());
            }

            // Page layout information
            String pageLayout = "Unknown";
            switch (document.getDocumentCatalog().getPageLayout()) {
                case org.apache.pdfbox.pdmodel.PageLayout.SINGLE_PAGE:
                    pageLayout = "SinglePage";
                    break;
                case org.apache.pdfbox.pdmodel.PageLayout.ONE_COLUMN:
                    pageLayout = "OneColumn";
                    break;
                case org.apache.pdfbox.pdmodel.PageLayout.TWO_COLUMN_LEFT:
                    pageLayout = "TwoColumnLeft";
                    break;
                case org.apache.pdfbox.pdmodel.PageLayout.TWO_COLUMN_RIGHT:
                    pageLayout = "TwoColumnRight";
                    break;
                case org.apache.pdfbox.pdmodel.PageLayout.TWO_PAGE_LEFT:
                    pageLayout = "TwoPageLeft";
                    break;
                case org.apache.pdfbox.pdmodel.PageLayout.TWO_PAGE_RIGHT:
                    pageLayout = "TwoPageRight";
                    break;
            }
            metadata.put("PageLayout", pageLayout);

            // Page mode information
            String pageMode = "Unknown";
            switch (document.getDocumentCatalog().getPageMode()) {
                case org.apache.pdfbox.pdmodel.PageMode.USE_NONE:
                    pageMode = "UseNone";
                    break;
                case org.apache.pdfbox.pdmodel.PageMode.USE_OUTLINES:
                    pageMode = "UseOutlines";
                    break;
                case org.apache.pdfbox.pdmodel.PageMode.USE_THUMBS:
                    pageMode = "UseThumbs";
                    break;
                case org.apache.pdfbox.pdmodel.PageMode.FULL_SCREEN:
                    pageMode = "FullScreen";
                    break;
                case org.apache.pdfbox.pdmodel.PageMode.USE_OPTIONAL_CONTENT:
                    pageMode = "UseOptionalContent";
                    break;
                case org.apache.pdfbox.pdmodel.PageMode.USE_ATTACHMENTS:
                    pageMode = "UseAttachments";
                    break;
            }
            metadata.put("PageMode", pageMode);

            // Check for form fields
            boolean hasForms = document.getDocumentCatalog().getAcroForm() != null && 
                              document.getDocumentCatalog().getAcroForm().getFields() != null && 
                              !document.getDocumentCatalog().getAcroForm().getFields().isEmpty();
            metadata.put("HasFormFields", String.valueOf(hasForms));

            // Check for outlines/bookmarks
            boolean hasOutlines = document.getDocumentCatalog().getDocumentOutline() != null && 
                                 document.getDocumentCatalog().getDocumentOutline().hasChildren();
            metadata.put("HasOutlines", String.valueOf(hasOutlines));

            return metadata;
        } catch (Exception e) {
            logger.error("Error extracting metadata", e);
            return metadata;
        }
    }

    /**
     * Creates PdfPage objects for each page in a PDF document.
     *
     * @param document The PDF document
     * @param isImageBased Whether the document is image-based
     * @return A list of PdfPage objects
     */
    private List<PdfPage> createPdfPages(PDDocument document, boolean isImageBased) {
        List<PdfPage> pages = new ArrayList<>();
        PDPageTree pageTree = document.getPages();

        for (int i = 0; i < pageTree.getCount(); i++) {
            try {
                PDPage pdPage = pageTree.get(i);
                PDRectangle mediaBox = pdPage.getMediaBox();

                // Create a PdfPage object
                PdfPage page = new PdfPage.Builder()
                        .pageNumber(i + 1)
                        .size((int) mediaBox.getWidth(), (int) mediaBox.getHeight())
                        .hasText(!isImageBased)
                        .isImageBased(isImageBased)
                        .rotation(pdPage.getRotation())
                        .build();

                pages.add(page);
            } catch (Exception e) {
                logger.error("Error creating PdfPage for page {}", i + 1, e);

                // Add a placeholder page
                PdfPage placeholderPage = new PdfPage.Builder()
                        .pageNumber(i + 1)
                        .size(612, 792) // Default letter size
                        .hasText(false)
                        .build();

                pages.add(placeholderPage);
            }
        }

        return pages;
    }

    /**
     * Pre-renders adjacent pages in the background to improve navigation performance.
     *
     * @param document The PDF document
     * @param currentPage The current page number
     * @param dpi The DPI to render at
     */
    private void preRenderAdjacentPages(PdfDocument document, int currentPage, float dpi) {
        String filePath = document.getFilePath();
        PdfRenderer renderer = rendererCache.get(filePath);

        if (renderer == null) {
            return;
        }

        // Pre-render the next and previous pages
        List<Integer> pagesToPreRender = new ArrayList<>();

        if (currentPage < document.getPageCount()) {
            pagesToPreRender.add(currentPage + 1);
        }

        if (currentPage > 1) {
            pagesToPreRender.add(currentPage - 1);
        }

        // Use virtual threads to pre-render pages in parallel
        for (int pageNumber : pagesToPreRender) {
            final int pageToRender = pageNumber;
            executor.submit(() -> {
                try {
                    // Check if the page is already cached
                    Optional<BufferedImage> cachedImage = enhancedPageCache.getPage(filePath, pageToRender);
                    if (cachedImage.isPresent()) {
                        return;
                    }

                    logger.debug("Pre-rendering page {}", pageToRender);

                    // Render the page using our renderer
                    BufferedImage image = renderer.renderImageWithDPI(pageToRender - 1, dpi);

                    // Cache the rendered page
                    enhancedPageCache.putPage(filePath, pageToRender, image);
                } catch (Exception e) {
                    logger.error("Error pre-rendering page {}", pageToRender, e);
                }
            });
        }
    }
}
