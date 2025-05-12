package com.pdfviewer.model.service.font;

import com.pdfviewer.model.entity.CustomFont;
import com.pdfviewer.model.entity.UserSetting;
import com.pdfviewer.model.repository.CustomFontRepository;
import com.pdfviewer.model.repository.UserSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for font-related operations.
 * This class handles loading custom fonts, optimizing font rendering, and adjusting font size.
 */
public class FontManager {
    private static final Logger logger = LoggerFactory.getLogger(FontManager.class);

    // Default font sizes
    private static final int DEFAULT_FONT_SIZE = 12;
    private static final int MIN_FONT_SIZE = 8;
    private static final int MAX_FONT_SIZE = 24;

    // Font cache to avoid reloading fonts
    private final Map<String, Font> fontCache = new ConcurrentHashMap<>();

    // Repositories
    private final CustomFontRepository customFontRepository;
    private final UserSettingRepository userSettingRepository;

    // Current font size
    private int currentFontSize = DEFAULT_FONT_SIZE;

    /**
     * Creates a new font manager.
     * 
     * @param customFontRepository the repository for custom fonts
     * @param userSettingRepository the repository for user settings
     */
    public FontManager(CustomFontRepository customFontRepository, UserSettingRepository userSettingRepository) {
        this.customFontRepository = customFontRepository;
        this.userSettingRepository = userSettingRepository;

        // Load font size from settings
        try {
            String fontSizeStr = userSettingRepository.getValueOrDefault(
                    UserSetting.SettingKey.FONT_SIZE, String.valueOf(DEFAULT_FONT_SIZE));
            currentFontSize = Integer.parseInt(fontSizeStr);
        } catch (SQLException | NumberFormatException e) {
            logger.warn("Failed to load font size from settings, using default", e);
            currentFontSize = DEFAULT_FONT_SIZE;
        }

        // Initialize font rendering optimization
        initializeFontRendering();
    }

    /**
     * Initializes font rendering optimization.
     */
    private void initializeFontRendering() {
        // Enable font anti-aliasing
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // Load custom fonts
        loadCustomFonts();
    }

    /**
     * Loads all active custom fonts.
     */
    private void loadCustomFonts() {
        try {
            List<CustomFont> customFonts = customFontRepository.findAllActive();
            for (CustomFont customFont : customFonts) {
                loadFont(customFont);
            }
            logger.info("Loaded {} custom fonts", customFonts.size());
        } catch (SQLException e) {
            logger.error("Failed to load custom fonts", e);
        }
    }

    /**
     * Loads a custom font.
     * 
     * @param customFont the custom font to load
     */
    private void loadFont(CustomFont customFont) {
        if (fontCache.containsKey(customFont.getId())) {
            logger.debug("Font already loaded: {}", customFont.getFontName());
            return;
        }

        try {
            File fontFile = new File(customFont.getFontPath());
            if (!fontFile.exists() || !fontFile.isFile()) {
                logger.warn("Font file not found: {}", customFont.getFontPath());
                return;
            }

            Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile);
            fontCache.put(customFont.getId(), font);

            // Register the font with the graphics environment
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(font);

            logger.info("Loaded font: {}", customFont.getFontName());
        } catch (IOException | FontFormatException e) {
            logger.error("Failed to load font: {}", customFont.getFontPath(), e);
        }
    }

    /**
     * Adds a custom font.
     * 
     * @param fontName the name of the font
     * @param fontPath the path to the font file
     * @return the added custom font
     * @throws SQLException if a database access error occurs
     * @throws IOException if an I/O error occurs
     * @throws FontFormatException if the font file has an unsupported format
     */
    public CustomFont addCustomFont(String fontName, String fontPath) throws SQLException, IOException, FontFormatException {
        // Validate the font file
        File fontFile = new File(fontPath);
        if (!fontFile.exists() || !fontFile.isFile()) {
            throw new IOException("Font file not found: " + fontPath);
        }

        // Try to load the font to validate it
        Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile);

        // Create and save the custom font
        CustomFont customFont = new CustomFont.Builder()
                .fontName(fontName)
                .fontPath(fontPath)
                .build();

        CustomFont savedFont = customFontRepository.save(customFont);

        // Add to cache
        fontCache.put(savedFont.getId(), font);

        // Register the font with the graphics environment
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        ge.registerFont(font);

        logger.info("Added custom font: {}", fontName);

        return savedFont;
    }

    /**
     * Gets all custom fonts.
     * 
     * @return a list of all custom fonts
     * @throws SQLException if a database access error occurs
     */
    public List<CustomFont> getAllCustomFonts() throws SQLException {
        return customFontRepository.findAll();
    }

    /**
     * Gets all active custom fonts.
     * 
     * @return a list of all active custom fonts
     * @throws SQLException if a database access error occurs
     */
    public List<CustomFont> getActiveCustomFonts() throws SQLException {
        return customFontRepository.findAllActive();
    }

    /**
     * Sets the active state of a custom font.
     * 
     * @param fontId the ID of the font to update
     * @param active the new active state
     * @return true if the font was updated, false if the font was not found
     * @throws SQLException if a database access error occurs
     */
    public boolean setFontActive(String fontId, boolean active) throws SQLException {
        return customFontRepository.setActive(fontId, active);
    }

    /**
     * Deletes a custom font.
     * 
     * @param fontId the ID of the font to delete
     * @throws SQLException if a database access error occurs
     */
    public void deleteCustomFont(String fontId) throws SQLException {
        customFontRepository.deleteById(fontId);
        fontCache.remove(fontId);
        logger.info("Deleted custom font: {}", fontId);
    }

    /**
     * Gets a font by its ID.
     * 
     * @param fontId the ID of the font to get
     * @return the font, or null if not found
     */
    public Font getFont(String fontId) {
        return fontCache.get(fontId);
    }

    /**
     * Gets a font by its name.
     * 
     * @param fontName the name of the font to get
     * @return the font, or null if not found
     */
    public Font getFontByName(String fontName) {
        try {
            List<CustomFont> fonts = customFontRepository.findByName(fontName);
            if (!fonts.isEmpty()) {
                String fontId = fonts.get(0).getId();
                return getFont(fontId);
            }
        } catch (SQLException e) {
            logger.error("Failed to get font by name: {}", fontName, e);
        }
        return null;
    }

    /**
     * Gets the current font size.
     * 
     * @return the current font size
     */
    public int getCurrentFontSize() {
        return currentFontSize;
    }

    /**
     * Sets the font size.
     * 
     * @param fontSize the new font size
     * @throws SQLException if a database access error occurs
     */
    public void setFontSize(int fontSize) throws SQLException {
        if (fontSize < MIN_FONT_SIZE || fontSize > MAX_FONT_SIZE) {
            throw new IllegalArgumentException("Font size must be between " + MIN_FONT_SIZE + " and " + MAX_FONT_SIZE);
        }

        currentFontSize = fontSize;

        // Save to settings
        userSettingRepository.setValue(UserSetting.SettingKey.FONT_SIZE, String.valueOf(fontSize));

        logger.info("Font size set to {}", fontSize);
    }

    /**
     * Increases the font size.
     * 
     * @throws SQLException if a database access error occurs
     */
    public void increaseFontSize() throws SQLException {
        if (currentFontSize < MAX_FONT_SIZE) {
            setFontSize(currentFontSize + 1);
        }
    }

    /**
     * Decreases the font size.
     * 
     * @throws SQLException if a database access error occurs
     */
    public void decreaseFontSize() throws SQLException {
        if (currentFontSize > MIN_FONT_SIZE) {
            setFontSize(currentFontSize - 1);
        }
    }

    /**
     * Resets the font size to the default.
     * 
     * @throws SQLException if a database access error occurs
     */
    public void resetFontSize() throws SQLException {
        setFontSize(DEFAULT_FONT_SIZE);
    }

    /**
     * Gets a derived font with the current font size.
     * 
     * @param font the base font
     * @return the derived font with the current font size
     */
    public Font getDerivedFont(Font font) {
        return font.deriveFont((float) currentFontSize);
    }

    /**
     * Gets a derived font with the specified font size.
     * 
     * @param font the base font
     * @param fontSize the font size
     * @return the derived font with the specified font size
     */
    public Font getDerivedFontWithSize(Font font, int fontSize) {
        return font.deriveFont((float) fontSize);
    }

    /**
     * Gets a derived font with the specified style.
     * 
     * @param font the base font
     * @param style the font style (e.g., Font.BOLD)
     * @return the derived font with the specified style
     */
    public Font getDerivedFont(Font font, int style) {
        return font.deriveFont(style);
    }

    /**
     * Gets a derived font with the specified style and font size.
     * 
     * @param font the base font
     * @param style the font style (e.g., Font.BOLD)
     * @param fontSize the font size
     * @return the derived font with the specified style and font size
     */
    public Font getDerivedFont(Font font, int style, int fontSize) {
        return font.deriveFont(style, (float) fontSize);
    }
}
