package com.pdfviewer.model.entity;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a PDF document with its metadata and pages.
 * This is an immutable class that holds all the information about a PDF document.
 */
public final class PdfDocument {
    private final String filePath;
    private final String fileName;
    private final int pageCount;
    private final PdfVersion version;
    private final Map<String, String> metadata;
    private final List<PdfPage> pages;
    private final LocalDateTime openedAt;
    private final boolean isImageBased;
    private final boolean isRepaired;

    private PdfDocument(Builder builder) {
        this.filePath = builder.filePath;
        this.fileName = builder.fileName;
        this.pageCount = builder.pageCount;
        this.version = builder.version;
        this.metadata = Collections.unmodifiableMap(builder.metadata);
        this.pages = Collections.unmodifiableList(builder.pages);
        this.openedAt = builder.openedAt;
        this.isImageBased = builder.isImageBased;
        this.isRepaired = builder.isRepaired;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public int getPageCount() {
        return pageCount;
    }

    public PdfVersion getVersion() {
        return version;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public List<PdfPage> getPages() {
        return pages;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public boolean isImageBased() {
        return isImageBased;
    }

    public boolean isRepaired() {
        return isRepaired;
    }

    public Optional<PdfPage> getPage(int pageNumber) {
        if (pageNumber < 1 || pageNumber > pageCount) {
            return Optional.empty();
        }
        return Optional.of(pages.get(pageNumber - 1));
    }

    /**
     * Builder for creating PdfDocument instances.
     */
    public static class Builder {
        private String filePath;
        private String fileName;
        private int pageCount;
        private PdfVersion version;
        private Map<String, String> metadata = Collections.emptyMap();
        private List<PdfPage> pages = Collections.emptyList();
        private LocalDateTime openedAt = LocalDateTime.now();
        private boolean isImageBased = false;
        private boolean isRepaired = false;

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder filePath(Path path) {
            this.filePath = path.toString();
            this.fileName = path.getFileName().toString();
            return this;
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder pageCount(int pageCount) {
            this.pageCount = pageCount;
            return this;
        }

        public Builder version(PdfVersion version) {
            this.version = version;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder pages(List<PdfPage> pages) {
            this.pages = pages;
            return this;
        }

        public Builder openedAt(LocalDateTime openedAt) {
            this.openedAt = openedAt;
            return this;
        }

        public Builder isImageBased(boolean isImageBased) {
            this.isImageBased = isImageBased;
            return this;
        }

        public Builder isRepaired(boolean isRepaired) {
            this.isRepaired = isRepaired;
            return this;
        }

        public PdfDocument build() {
            return new PdfDocument(this);
        }
    }

    /**
     * Represents a PDF version with major and minor numbers.
     */
    public record PdfVersion(int major, int minor) {
        @Override
        public String toString() {
            return major + "." + minor;
        }
    }
}
