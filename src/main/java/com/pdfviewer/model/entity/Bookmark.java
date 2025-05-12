package com.pdfviewer.model.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a bookmark in a PDF document.
 * This class is used to store information about bookmarks created by the user.
 */
public class Bookmark {
    private final String id;
    private final String fileId;
    private final int pageNumber;
    private final String name;
    private final String description;
    private final LocalDateTime createdAt;
    
    private Bookmark(Builder builder) {
        this.id = builder.id;
        this.fileId = builder.fileId;
        this.pageNumber = builder.pageNumber;
        this.name = builder.name;
        this.description = builder.description;
        this.createdAt = builder.createdAt;
    }
    
    public String getId() {
        return id;
    }
    
    public String getFileId() {
        return fileId;
    }
    
    public int getPageNumber() {
        return pageNumber;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bookmark bookmark = (Bookmark) o;
        return Objects.equals(id, bookmark.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Bookmark{" +
                "id='" + id + '\'' +
                ", fileId='" + fileId + '\'' +
                ", pageNumber=" + pageNumber +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
    
    /**
     * Builder for creating Bookmark instances.
     */
    public static class Builder {
        private String id = UUID.randomUUID().toString();
        private String fileId;
        private int pageNumber;
        private String name;
        private String description;
        private LocalDateTime createdAt = LocalDateTime.now();
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder fileId(String fileId) {
            this.fileId = fileId;
            return this;
        }
        
        public Builder pageNumber(int pageNumber) {
            this.pageNumber = pageNumber;
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public Bookmark build() {
            return new Bookmark(this);
        }
    }
}