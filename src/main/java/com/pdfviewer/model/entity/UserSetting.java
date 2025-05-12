package com.pdfviewer.model.entity;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a user setting in the application.
 * This class is used to store user preferences and application settings.
 */
public class UserSetting {
    private final String key;
    private final String value;
    private final LocalDateTime updatedAt;
    
    private UserSetting(Builder builder) {
        this.key = builder.key;
        this.value = builder.value;
        this.updatedAt = builder.updatedAt;
    }
    
    public String getKey() {
        return key;
    }
    
    public String getValue() {
        return value;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    /**
     * Creates a new UserSetting with the same key but a different value.
     * 
     * @param newValue the new value for the setting
     * @return a new UserSetting instance with the updated value
     */
    public UserSetting withValue(String newValue) {
        return new Builder()
                .key(this.key)
                .value(newValue)
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserSetting that = (UserSetting) o;
        return Objects.equals(key, that.key);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
    
    @Override
    public String toString() {
        return "UserSetting{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", updatedAt=" + updatedAt +
                '}';
    }
    
    /**
     * Common setting keys used in the application.
     * This enum provides a centralized place to define all setting keys,
     * which helps prevent typos and ensures consistency.
     */
    public enum SettingKey {
        THEME("theme"),
        DEFAULT_ZOOM("default_zoom"),
        SHOW_THUMBNAILS("show_thumbnails"),
        REMEMBER_LAST_PAGE("remember_last_page"),
        RECENT_FILES_LIMIT("recent_files_limit"),
        DEFAULT_VIEW_MODE("default_view_mode"),
        ENABLE_OCR("enable_ocr"),
        HIGHLIGHT_COLOR("highlight_color"),
        FONT_SIZE("font_size"),
        LANGUAGE("language");
        
        private final String key;
        
        SettingKey(String key) {
            this.key = key;
        }
        
        public String getKey() {
            return key;
        }
    }
    
    /**
     * Builder for creating UserSetting instances.
     */
    public static class Builder {
        private String key;
        private String value;
        private LocalDateTime updatedAt = LocalDateTime.now();
        
        public Builder key(String key) {
            this.key = key;
            return this;
        }
        
        public Builder key(SettingKey key) {
            this.key = key.getKey();
            return this;
        }
        
        public Builder value(String value) {
            this.value = value;
            return this;
        }
        
        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        
        public UserSetting build() {
            return new UserSetting(this);
        }
    }
}