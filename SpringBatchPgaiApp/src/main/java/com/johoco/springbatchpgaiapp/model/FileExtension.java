package com.johoco.springbatchpgaiapp.model;

import org.apache.commons.io.FilenameUtils;

import java.io.File;

/**
 * Enum representing supported file extensions in the application.
 */
public enum FileExtension {
    PDF("pdf"),
    TXT("txt"),
    UNKNOWN("unknown");

    private final String extension;

    FileExtension(String extension) {
        this.extension = extension;
    }

    /**
     * Gets the string representation of the extension (lowercase, without dot).
     *
     * @return the extension string
     */
    public String getValue() {
        return extension;
    }

    /**
     * Determines the FileExtension enum value for a given file.
     *
     * @param file the file to check
     * @return the corresponding FileExtension, or UNKNOWN if not recognized
     */
    public static FileExtension fromFile(File file) {
        if (file == null) {
            return UNKNOWN;
        }
        
        String extension = FilenameUtils.getExtension(file.getName()).toLowerCase();
        
        for (FileExtension fileExtension : values()) {
            if (fileExtension.extension.equals(extension)) {
                return fileExtension;
            }
        }
        
        return UNKNOWN;
    }
    
    /**
     * Determines the FileExtension enum value for a given extension string.
     *
     * @param extension the extension string (with or without leading dot)
     * @return the corresponding FileExtension, or UNKNOWN if not recognized
     */
    public static FileExtension fromString(String extension) {
        if (extension == null || extension.isEmpty()) {
            return UNKNOWN;
        }
        
        // Remove leading dot if present
        String cleanExtension = extension.startsWith(".") 
            ? extension.substring(1).toLowerCase() 
            : extension.toLowerCase();
        
        for (FileExtension fileExtension : values()) {
            if (fileExtension.extension.equals(cleanExtension)) {
                return fileExtension;
            }
        }
        
        return UNKNOWN;
    }
    
    /**
     * Checks if this extension is equal to the given string.
     *
     * @param extension the extension string to compare with
     * @return true if they are equal, false otherwise
     */
    public boolean matches(String extension) {
        if (extension == null) {
            return false;
        }
        
        String cleanExtension = extension.startsWith(".") 
            ? extension.substring(1).toLowerCase() 
            : extension.toLowerCase();
            
        return this.extension.equals(cleanExtension);
    }
}
