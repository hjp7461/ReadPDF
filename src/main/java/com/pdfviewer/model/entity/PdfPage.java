package com.pdfviewer.model.entity;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Optional;

/**
 * Represents a single page in a PDF document.
 * This class is immutable and holds information about a page's properties and content.
 */
public final class PdfPage {
    private final int pageNumber;
    private final Dimension size;
    private final boolean hasText;
    private final BufferedImage thumbnail;
    private final float rotation;
    private final boolean isImageBased;

    private PdfPage(Builder builder) {
        this.pageNumber = builder.pageNumber;
        this.size = builder.size;
        this.hasText = builder.hasText;
        this.thumbnail = builder.thumbnail;
        this.rotation = builder.rotation;
        this.isImageBased = builder.isImageBased;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public Dimension getSize() {
        return new Dimension(size);
    }

    public boolean hasText() {
        return hasText;
    }

    public Optional<BufferedImage> getThumbnail() {
        return Optional.ofNullable(thumbnail);
    }

    public float getRotation() {
        return rotation;
    }

    public boolean isImageBased() {
        return isImageBased;
    }

    /**
     * Builder for creating PdfPage instances.
     */
    public static class Builder {
        private int pageNumber;
        private Dimension size;
        private boolean hasText = true;
        private BufferedImage thumbnail;
        private float rotation = 0.0f;
        private boolean isImageBased = false;

        public Builder pageNumber(int pageNumber) {
            this.pageNumber = pageNumber;
            return this;
        }

        public Builder size(Dimension size) {
            this.size = new Dimension(size);
            return this;
        }

        public Builder size(int width, int height) {
            this.size = new Dimension(width, height);
            return this;
        }

        public Builder hasText(boolean hasText) {
            this.hasText = hasText;
            return this;
        }

        public Builder thumbnail(BufferedImage thumbnail) {
            this.thumbnail = thumbnail;
            return this;
        }

        public Builder rotation(float rotation) {
            this.rotation = rotation;
            return this;
        }

        public Builder isImageBased(boolean isImageBased) {
            this.isImageBased = isImageBased;
            return this;
        }

        public PdfPage build() {
            return new PdfPage(this);
        }
    }
}