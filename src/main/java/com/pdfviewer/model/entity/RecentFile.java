package com.pdfviewer.model.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a recently opened PDF file.
 * This class is used to store information about files that have been recently opened by the user.
 */
public class RecentFile {
    private final String id;
    private final String filePath;
    private final String fileName;
    private final LocalDateTime lastOpened;
    private final int pageCount;
    private final int lastPageViewed;
    private final float lastZoomLevel;
    
    private RecentFile(Builder builder) {
        this.id = builder.id;
        this.filePath = builder.filePath;
        this.fileName = builder.fileName;
        this.lastOpened = builder.lastOpened;
        this.pageCount = builder.pageCount;
        this.lastPageViewed = builder.lastPageViewed;
        this.lastZoomLevel = builder.lastZoomLevel;
    }
    
    public String getId() {
        return id;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public LocalDateTime getLastOpened() {
        return lastOpened;
    }
    
    public int getPageCount() {
        return pageCount;
    }
    
    public int getLastPageViewed() {
        return lastPageViewed;
    }
    
    public float getLastZoomLevel() {
        return lastZoomLevel;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecentFile that = (RecentFile) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "RecentFile{" +
                "id='" + id + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileName='" + fileName + '\'' +
                ", lastOpened=" + lastOpened +
                ", pageCount=" + pageCount +
                ", lastPageViewed=" + lastPageViewed +
                ", lastZoomLevel=" + lastZoomLevel +
                '}';
    }
    
    /**
     * Creates a new RecentFile from a PdfDocument.
     * 
     * @param document the PDF document
     * @param lastPageViewed the last page viewed by the user
     * @param lastZoomLevel the last zoom level used by the user
     * @return a new RecentFile instance
     */
    public static RecentFile fromPdfDocument(PdfDocument document, int lastPageViewed, float lastZoomLevel) {
        return new Builder()
                .id(UUID.randomUUID().toString())
                .filePath(document.getFilePath())
                .fileName(document.getFileName())
                .lastOpened(LocalDateTime.now())
                .pageCount(document.getPageCount())
                .lastPageViewed(lastPageViewed)
                .lastZoomLevel(lastZoomLevel)
                .build();
    }
    
    /**
     * Builder for creating RecentFile instances.
     */
    public static class Builder {
        private String id = UUID.randomUUID().toString();
        private String filePath;
        private String fileName;
        private LocalDateTime lastOpened = LocalDateTime.now();
        private int pageCount;
        private int lastPageViewed = 1;
        private float lastZoomLevel = 100.0f;
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }
        
        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }
        
        public Builder lastOpened(LocalDateTime lastOpened) {
            this.lastOpened = lastOpened;
            return this;
        }
        
        public Builder pageCount(int pageCount) {
            this.pageCount = pageCount;
            return this;
        }
        
        public Builder lastPageViewed(int lastPageViewed) {
            this.lastPageViewed = lastPageViewed;
            return this;
        }
        
        public Builder lastZoomLevel(float lastZoomLevel) {
            this.lastZoomLevel = lastZoomLevel;
            return this;
        }
        
        public RecentFile build() {
            return new RecentFile(this);
        }
    }
}