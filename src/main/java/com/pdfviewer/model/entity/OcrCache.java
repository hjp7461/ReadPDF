package com.pdfviewer.model.entity;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a cached OCR result for a page in a PDF document.
 * This class is used to store extracted text from PDF pages to avoid repeated OCR processing.
 */
public class OcrCache {
    private final String fileId;
    private final int pageNumber;
    private final String textContent;
    private final LocalDateTime createdAt;
    
    private OcrCache(Builder builder) {
        this.fileId = builder.fileId;
        this.pageNumber = builder.pageNumber;
        this.textContent = builder.textContent;
        this.createdAt = builder.createdAt;
    }
    
    public String getFileId() {
        return fileId;
    }
    
    public int getPageNumber() {
        return pageNumber;
    }
    
    public String getTextContent() {
        return textContent;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OcrCache ocrCache = (OcrCache) o;
        return pageNumber == ocrCache.pageNumber && 
               Objects.equals(fileId, ocrCache.fileId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(fileId, pageNumber);
    }
    
    @Override
    public String toString() {
        return "OcrCache{" +
                "fileId='" + fileId + '\'' +
                ", pageNumber=" + pageNumber +
                ", textContentLength=" + (textContent != null ? textContent.length() : 0) +
                ", createdAt=" + createdAt +
                '}';
    }
    
    /**
     * Builder for creating OcrCache instances.
     */
    public static class Builder {
        private String fileId;
        private int pageNumber;
        private String textContent;
        private LocalDateTime createdAt = LocalDateTime.now();
        
        public Builder fileId(String fileId) {
            this.fileId = fileId;
            return this;
        }
        
        public Builder pageNumber(int pageNumber) {
            this.pageNumber = pageNumber;
            return this;
        }
        
        public Builder textContent(String textContent) {
            this.textContent = textContent;
            return this;
        }
        
        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public OcrCache build() {
            return new OcrCache(this);
        }
    }
}