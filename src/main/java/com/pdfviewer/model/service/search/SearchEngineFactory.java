package com.pdfviewer.model.service.search;

import com.pdfviewer.model.service.PdfService;

/**
 * Factory for creating SearchEngine instances.
 */
public class SearchEngineFactory {
    
    private final PdfService pdfService;
    
    public SearchEngineFactory(PdfService pdfService) {
        this.pdfService = pdfService;
    }
    
    /**
     * Creates a new SearchEngine instance.
     *
     * @return A new SearchEngine instance
     */
    public SearchEngine createSearchEngine() {
        return new PdfSearchEngine(pdfService);
    }
}