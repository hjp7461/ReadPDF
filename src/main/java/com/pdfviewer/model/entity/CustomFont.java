/**
 * Represents a custom font in the application.
 * This class is used to store information about custom fonts added by the user.
 */
package com.pdfviewer.model.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class CustomFont {
    private final String id;
    private final String fontName;
    private final String fontPath;
    private final boolean isActive;
    private final LocalDateTime createdAt;
    
    private CustomFont(Builder builder) {
        this.id = builder.id;
        this.fontName = builder.fontName;
        this.fontPath = builder.fontPath;
        this.isActive = builder.isActive;
        this.createdAt = builder.createdAt;
    }
    
    public String getId() {
        return id;
    }
    
    public String getFontName() {
        return fontName;
    }
    
    public String getFontPath() {
        return fontPath;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Creates a new CustomFont with the same properties but a different active state.
     * 
     * @param active the new active state
     * @return a new CustomFont instance with the updated active state
     */
    public CustomFont withActive(boolean active) {
        return new Builder()
                .id(this.id)
                .fontName(this.fontName)
                .fontPath(this.fontPath)
                .isActive(active)
                .createdAt(this.createdAt)
                .build();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomFont that = (CustomFont) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "CustomFont{" +
                "id='" + id + '\'' +
                ", fontName='" + fontName + '\'' +
                ", fontPath='" + fontPath + '\'' +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                '}';
    }
    
    /**
     * Builder for creating CustomFont instances.
     */
    public static class Builder {
        private String id = UUID.randomUUID().toString();
        private String fontName;
        private String fontPath;
        private boolean isActive = true;
        private LocalDateTime createdAt = LocalDateTime.now();
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder fontName(String fontName) {
            this.fontName = fontName;
            return this;
        }
        
        public Builder fontPath(String fontPath) {
            this.fontPath = fontPath;
            return this;
        }
        
        public Builder isActive(boolean isActive) {
            this.isActive = isActive;
            return this;
        }
        
        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public CustomFont build() {
            return new CustomFont(this);
        }
    }
}