package com.johoco.springbatchpgaiapp.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Service class for file operations.
 */
@Slf4j
@Service
public class FileOperations {

    /**
     * Reads content from a file.
     *
     * @param file the file to read
     * @return the content of the file as a string
     * @throws IOException if an I/O error occurs
     */
    public String readFileContent(File file) throws IOException {
        if (file == null) {
            log.error("Cannot read content from null file");
            throw new IllegalArgumentException("File cannot be null");
        }
        
        try {
            log.debug("Reading content from file: {}", file.getName());
            String content = Files.readString(file.toPath());
            log.debug("Successfully read {} characters from file: {}", content.length(), file.getName());
            return content;
        } catch (IOException e) {
            log.error("Error reading file {}: {}", file.getName(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Gets the file size in bytes.
     *
     * @param file the file to get size for
     * @return the size of the file in bytes
     */
    public long getFileSize(File file) {
        if (file == null) {
            log.error("Cannot get size from null file");
            throw new IllegalArgumentException("File cannot be null");
        }
        
        return file.length();
    }
    
    /**
     * Gets the last modified timestamp of the file.
     *
     * @param file the file to get last modified timestamp for
     * @return the last modified timestamp in milliseconds since epoch
     */
    public long getLastModified(File file) {
        if (file == null) {
            log.error("Cannot get last modified timestamp from null file");
            throw new IllegalArgumentException("File cannot be null");
        }
        
        return file.lastModified();
    }
    
    /**
     * Ensures that a directory exists, creating it if necessary.
     *
     * @param directoryPath the path to the directory
     * @return the directory File object
     */
    public File ensureDirectoryExists(String directoryPath) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            log.error("Cannot ensure directory exists with null or empty path");
            throw new IllegalArgumentException("Directory path cannot be null or empty");
        }
        
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            log.info("Directory does not exist, creating: {}", directoryPath);
            boolean created = directory.mkdirs();
            if (created) {
                log.info("Successfully created directory: {}", directoryPath);
            } else {
                log.warn("Failed to create directory: {}", directoryPath);
            }
        }
        
        return directory;
    }
}
