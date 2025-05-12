package com.pdfviewer.model.service.search;

import com.pdfviewer.model.entity.PdfDocument;
import com.pdfviewer.model.entity.PdfPage;
import com.pdfviewer.model.entity.SearchResult;
import com.pdfviewer.model.service.PdfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.awt.Dimension;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PdfSearchEngineTest {

    @Mock
    private PdfService pdfService;

    private SearchEngine searchEngine;
    private PdfDocument testDocument;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        searchEngine = new PdfSearchEngine(pdfService);
        
        // Create a test document with 3 pages
        PdfPage page1 = new PdfPage.Builder()
                .pageNumber(1)
                .size(new Dimension(595, 842))
                .hasText(true)
                .build();
        
        PdfPage page2 = new PdfPage.Builder()
                .pageNumber(2)
                .size(new Dimension(595, 842))
                .hasText(true)
                .build();
        
        PdfPage page3 = new PdfPage.Builder()
                .pageNumber(3)
                .size(new Dimension(595, 842))
                .hasText(true)
                .build();
        
        testDocument = new PdfDocument.Builder()
                .filePath("/test/document.pdf")
                .fileName("document.pdf")
                .pageCount(3)
                .pages(List.of(page1, page2, page3))
                .metadata(Map.of("Title", "Test Document"))
                .build();
        
        // Mock the text extraction
        when(pdfService.extractText(testDocument, 1)).thenReturn(
                "This is the first page of the test document. It contains some text to search for.");
        when(pdfService.extractText(testDocument, 2)).thenReturn(
                "This is the second page. It also contains some TEXT to search for, with different case.");
        when(pdfService.extractText(testDocument, 3)).thenReturn(
                "This is the third page with special characters: (test) [test] {test}.");
    }

    @Test
    void testBasicSearch() {
        // Search for a term that appears on all pages
        SearchResult result = searchEngine.search(testDocument, "test");
        
        assertEquals("test", result.getSearchTerm());
        assertEquals(3, result.getTotalMatches());
        assertFalse(result.isCaseSensitive());
        assertFalse(result.isUseRegex());
        
        // Verify page numbers
        assertEquals(1, result.getMatches().get(0).pageNumber());
        assertEquals(2, result.getMatches().get(1).pageNumber());
        assertEquals(3, result.getMatches().get(2).pageNumber());
    }

    @Test
    void testCaseSensitiveSearch() {
        // Search for "TEXT" with case sensitivity
        SearchResult result = searchEngine.search(testDocument, "TEXT", true, false);
        
        assertEquals("TEXT", result.getSearchTerm());
        assertEquals(1, result.getTotalMatches());
        assertTrue(result.isCaseSensitive());
        assertFalse(result.isUseRegex());
        
        // Verify it's only found on page 2
        assertEquals(2, result.getMatches().get(0).pageNumber());
    }

    @Test
    void testRegexSearch() {
        // Search for text inside parentheses
        SearchResult result = searchEngine.search(testDocument, "\\(test\\)", false, true);
        
        assertEquals("\\(test\\)", result.getSearchTerm());
        assertEquals(1, result.getTotalMatches());
        assertFalse(result.isCaseSensitive());
        assertTrue(result.isUseRegex());
        
        // Verify it's only found on page 3
        assertEquals(3, result.getMatches().get(0).pageNumber());
        assertEquals("(test)", result.getMatches().get(0).matchText());
    }

    @Test
    void testNavigateToNextResult() {
        SearchResult result = searchEngine.search(testDocument, "test");
        
        // Start at position 0, navigate to next
        int nextPos = searchEngine.navigateToNextResult(result, 0);
        assertEquals(1, nextPos);
        
        // Navigate from last position should wrap to 0
        nextPos = searchEngine.navigateToNextResult(result, 2);
        assertEquals(0, nextPos);
    }

    @Test
    void testNavigateToPreviousResult() {
        SearchResult result = searchEngine.search(testDocument, "test");
        
        // Start at position 1, navigate to previous
        int prevPos = searchEngine.navigateToPreviousResult(result, 1);
        assertEquals(0, prevPos);
        
        // Navigate from first position should wrap to last
        prevPos = searchEngine.navigateToPreviousResult(result, 0);
        assertEquals(2, prevPos);
    }

    @Test
    void testEmptySearchTerm() {
        SearchResult result = searchEngine.search(testDocument, "");
        
        assertEquals(0, result.getTotalMatches());
    }

    @Test
    void testInvalidRegexPattern() {
        // Search with an invalid regex pattern
        SearchResult result = searchEngine.search(testDocument, "[unclosed", false, true);
        
        assertEquals(0, result.getTotalMatches());
    }
}