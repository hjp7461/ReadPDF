package com.pdfviewer.model.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a search result in a PDF document.
 * This class is immutable and holds information about search matches in a document.
 */
public final class SearchResult {
    private final String searchTerm;
    private final int totalMatches;
    private final List<Match> matches;
    private final boolean caseSensitive;
    private final boolean useRegex;

    private SearchResult(Builder builder) {
        this.searchTerm = builder.searchTerm;
        this.matches = Collections.unmodifiableList(builder.matches);
        this.totalMatches = builder.matches.size();
        this.caseSensitive = builder.caseSensitive;
        this.useRegex = builder.useRegex;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public int getTotalMatches() {
        return totalMatches;
    }

    public List<Match> getMatches() {
        return matches;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public boolean isUseRegex() {
        return useRegex;
    }

    /**
     * Represents a single match in a search result.
     */
    public record Match(
            int pageNumber,
            String matchText,
            int startIndex,
            int endIndex,
            String surroundingText
    ) {
        /**
         * Returns the length of the match.
         */
        public int length() {
            return endIndex - startIndex;
        }
    }

    /**
     * Builder for creating SearchResult instances.
     */
    public static class Builder {
        private String searchTerm;
        private List<Match> matches = new ArrayList<>();
        private boolean caseSensitive = false;
        private boolean useRegex = false;

        public Builder searchTerm(String searchTerm) {
            this.searchTerm = searchTerm;
            return this;
        }

        public Builder matches(List<Match> matches) {
            this.matches = new ArrayList<>(matches);
            return this;
        }

        public Builder caseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
            return this;
        }

        public Builder useRegex(boolean useRegex) {
            this.useRegex = useRegex;
            return this;
        }

        public SearchResult build() {
            return new SearchResult(this);
        }
    }
}
