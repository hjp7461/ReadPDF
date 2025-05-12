package com.pdfviewer.model.service.search;

import com.pdfviewer.model.entity.PdfDocument;
import com.pdfviewer.model.entity.PdfPage;
import com.pdfviewer.model.entity.SearchResult;
import com.pdfviewer.model.service.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Implementation of the SearchEngine interface for PDF documents.
 */
public class PdfSearchEngine implements SearchEngine {
    private static final Logger logger = LoggerFactory.getLogger(PdfSearchEngine.class);
    private static final int SURROUNDING_TEXT_LENGTH = 50; // Characters before and after match

    private final PdfService pdfService;

    public PdfSearchEngine(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    @Override
    public SearchResult search(PdfDocument document, String searchTerm) {
        return search(document, searchTerm, false, false);
    }

    @Override
    public SearchResult search(PdfDocument document, String searchTerm, boolean caseSensitive, boolean useRegex) {
        if (document == null || searchTerm == null || searchTerm.isEmpty()) {
            return createEmptyResult(searchTerm, caseSensitive, useRegex);
        }

        logger.debug("Searching for '{}' in document: {} (case-sensitive: {}, regex: {})",
                searchTerm, document.getFileName(), caseSensitive, useRegex);

        List<SearchResult.Match> matches = new ArrayList<>();

        // Prepare pattern for regex search
        Pattern pattern = null;
        if (useRegex) {
            try {
                int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
                pattern = Pattern.compile(searchTerm, flags);
            } catch (PatternSyntaxException e) {
                logger.warn("Invalid regex pattern: {}", searchTerm, e);
                return createEmptyResult(searchTerm, caseSensitive, useRegex);
            }
        }

        // Search each page
        for (PdfPage page : document.getPages()) {
            if (!page.hasText()) {
                continue; // Skip pages without text
            }

            String pageText = pdfService.extractText(document, page.getPageNumber());
            if (pageText == null || pageText.isEmpty()) {
                continue;
            }

            if (useRegex && pattern != null) {
                findRegexMatches(matches, pattern, pageText, page.getPageNumber());
            } else {
                findTextMatches(matches, searchTerm, pageText, page.getPageNumber(), caseSensitive);
            }
        }

        logger.debug("Found {} matches for '{}'", matches.size(), searchTerm);

        return new SearchResult.Builder()
                .searchTerm(searchTerm)
                .matches(matches)
                .caseSensitive(caseSensitive)
                .useRegex(useRegex)
                .build();
    }

    @Override
    public boolean highlightSearchResults(PdfDocument document, SearchResult result) {
        // This would typically be implemented in the UI layer
        // For now, we'll just return true to indicate success
        logger.debug("Highlighting {} search results for '{}'", 
                result.getTotalMatches(), result.getSearchTerm());
        return true;
    }

    @Override
    public int navigateToNextResult(SearchResult result, int currentPosition) {
        if (result == null || result.getTotalMatches() == 0) {
            return currentPosition;
        }

        int nextPosition = currentPosition + 1;
        if (nextPosition >= result.getTotalMatches()) {
            nextPosition = 0; // Wrap around to the first result
        }

        logger.debug("Navigating from position {} to next result at position {}", 
                currentPosition, nextPosition);
        return nextPosition;
    }

    @Override
    public int navigateToPreviousResult(SearchResult result, int currentPosition) {
        if (result == null || result.getTotalMatches() == 0) {
            return currentPosition;
        }

        int prevPosition = currentPosition - 1;
        if (prevPosition < 0) {
            prevPosition = result.getTotalMatches() - 1; // Wrap around to the last result
        }

        logger.debug("Navigating from position {} to previous result at position {}", 
                currentPosition, prevPosition);
        return prevPosition;
    }

    private void findTextMatches(List<SearchResult.Match> matches, String searchTerm, 
                                String pageText, int pageNumber, boolean caseSensitive) {
        String textToSearch = pageText;
        String termToFind = searchTerm;

        if (!caseSensitive) {
            textToSearch = pageText.toLowerCase();
            termToFind = searchTerm.toLowerCase();
        }

        int index = 0;
        while ((index = textToSearch.indexOf(termToFind, index)) != -1) {
            String matchText = pageText.substring(index, index + searchTerm.length());
            String surroundingText = extractSurroundingText(pageText, index, searchTerm.length());

            matches.add(new SearchResult.Match(
                    pageNumber,
                    matchText,
                    index,
                    index + searchTerm.length(),
                    surroundingText
            ));

            index += searchTerm.length();
        }
    }

    private void findRegexMatches(List<SearchResult.Match> matches, Pattern pattern, 
                                 String pageText, int pageNumber) {
        Matcher matcher = pattern.matcher(pageText);

        while (matcher.find()) {
            String matchText = matcher.group();
            int startIndex = matcher.start();
            int endIndex = matcher.end();
            String surroundingText = extractSurroundingText(pageText, startIndex, matchText.length());

            matches.add(new SearchResult.Match(
                    pageNumber,
                    matchText,
                    startIndex,
                    endIndex,
                    surroundingText
            ));
        }
    }

    private String extractSurroundingText(String text, int matchStart, int matchLength) {
        int contextStart = Math.max(0, matchStart - SURROUNDING_TEXT_LENGTH);
        int contextEnd = Math.min(text.length(), matchStart + matchLength + SURROUNDING_TEXT_LENGTH);

        String prefix = "";
        if (contextStart > 0) {
            prefix = "...";
        }

        String suffix = "";
        if (contextEnd < text.length()) {
            suffix = "...";
        }

        return prefix + text.substring(contextStart, contextEnd).trim() + suffix;
    }

    private SearchResult createEmptyResult(String searchTerm, boolean caseSensitive, boolean useRegex) {
        return new SearchResult.Builder()
                .searchTerm(searchTerm)
                .matches(new ArrayList<>())
                .caseSensitive(caseSensitive)
                .useRegex(useRegex)
                .build();
    }
}
