package com.pdfviewer.model.service.search;

import com.pdfviewer.model.entity.PdfDocument;
import com.pdfviewer.model.entity.SearchResult;

/**
 * Interface for PDF document search operations.
 */
public interface SearchEngine {
    
    /**
     * Searches for text in the PDF document.
     *
     * @param document The PDF document to search in
     * @param searchTerm The text to search for
     * @return A SearchResult object containing the search results
     */
    SearchResult search(PdfDocument document, String searchTerm);
    
    /**
     * Searches for text in the PDF document with advanced options.
     *
     * @param document The PDF document to search in
     * @param searchTerm The text to search for
     * @param caseSensitive Whether the search should be case-sensitive
     * @param useRegex Whether the search term should be treated as a regular expression
     * @return A SearchResult object containing the search results
     */
    SearchResult search(PdfDocument document, String searchTerm, boolean caseSensitive, boolean useRegex);
    
    /**
     * Highlights search results in the document.
     *
     * @param document The PDF document containing the search results
     * @param result The search result to highlight
     * @return True if highlighting was successful, false otherwise
     */
    boolean highlightSearchResults(PdfDocument document, SearchResult result);
    
    /**
     * Navigates to the next search result.
     *
     * @param result The search result to navigate
     * @param currentPosition The current position (index) in the search results
     * @return The next position, or the same position if there is no next result
     */
    int navigateToNextResult(SearchResult result, int currentPosition);
    
    /**
     * Navigates to the previous search result.
     *
     * @param result The search result to navigate
     * @param currentPosition The current position (index) in the search results
     * @return The previous position, or the same position if there is no previous result
     */
    int navigateToPreviousResult(SearchResult result, int currentPosition);
}