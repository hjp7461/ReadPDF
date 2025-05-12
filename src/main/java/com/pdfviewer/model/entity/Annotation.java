package com.pdfviewer.model.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an annotation in a PDF document.
 * This class is used to store information about annotations created by the user.
 */
public class Annotation {
    private final String id;
    private final String fileId;
    private final int pageNumber;
    private final AnnotationType type;
    private final String content;
    private final float x;
    private final float y;
    private final Float width;
    private final Float height;
    private final String color;
    private final LocalDateTime createdAt;
    
    private Annotation(Builder builder) {
        this.id = builder.id;
        this.fileId = builder.fileId;
        this.pageNumber = builder.pageNumber;
        this.type = builder.type;
        this.content = builder.content;
        this.x = builder.x;
        this.y = builder.y;
        this.width = builder.width;
        this.height = builder.height;
        this.color = builder.color;
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
    
    public AnnotationType getType() {
        return type;
    }
    
    public String getContent() {
        return content;
    }
    
    public float getX() {
        return x;
    }
    
    public float getY() {
        return y;
    }
    
    public Float getWidth() {
        return width;
    }
    
    public Float getHeight() {
        return height;
    }
    
    public String getColor() {
        return color;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Annotation that = (Annotation) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Annotation{" +
                "id='" + id + '\'' +
                ", fileId='" + fileId + '\'' +
                ", pageNumber=" + pageNumber +
                ", type=" + type +
                ", content='" + content + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                ", color='" + color + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
    
    /**
     * Types of annotations supported by the application.
     */
    public enum AnnotationType {
        HIGHLIGHT,
        UNDERLINE,
        STRIKETHROUGH,
        NOTE,
        DRAWING,
        TEXT
    }
    
    /**
     * Builder for creating Annotation instances.
     */
    public static class Builder {
        private String id = UUID.randomUUID().toString();
        private String fileId;
        private int pageNumber;
        private AnnotationType type;
        private String content;
        private float x;
        private float y;
        private Float width;
        private Float height;
        private String color = "#FFFF00"; // Default yellow color
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
        
        public Builder type(AnnotationType type) {
            this.type = type;
            return this;
        }
        
        public Builder content(String content) {
            this.content = content;
            return this;
        }
        
        public Builder x(float x) {
            this.x = x;
            return this;
        }
        
        public Builder y(float y) {
            this.y = y;
            return this;
        }
        
        public Builder width(Float width) {
            this.width = width;
            return this;
        }
        
        public Builder height(Float height) {
            this.height = height;
            return this;
        }
        
        public Builder color(String color) {
            this.color = color;
            return this;
        }
        
        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public Annotation build() {
            return new Annotation(this);
        }
    }
}