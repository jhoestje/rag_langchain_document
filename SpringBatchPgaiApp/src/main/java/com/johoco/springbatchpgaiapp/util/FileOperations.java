package com.johoco.springbatchpgaiapp.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FilenameUtils;

/**
 * Service class for file operations.
 */
@Slf4j
@Service
public class FileOperations {

    /**
     * Reads content from a file.
     * 
     * Note: This method is kept for backward compatibility.
     * For file type-specific processing, use the appropriate FileProcessor.
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
            log.debug("Reading content from file using default method: {}", file.getName());
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
     * Ensures a directory exists, creating it if necessary.
     * 
     * @param directoryPath the path to the directory
     * @return the directory as a File object
     * @throws IOException if there is an error creating the directory
     */
    public File ensureDirectoryExists(String directoryPath) throws IOException {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            log.info("Creating directory: {}", directoryPath);
            if (!directory.mkdirs()) {
                throw new IOException("Failed to create directory: " + directoryPath);
            }
        } else if (!directory.isDirectory()) {
            throw new IOException("Path exists but is not a directory: " + directoryPath);
        }
        return directory;
    }
    
    /**
     * Moves a file to the processed directory.
     * 
     * @param file the file to move
     * @param outputDirectory the directory to move the file to
     * @return the moved file
     * @throws IOException if there is an error moving the file
     */
    public File moveToProcessed(File file, String outputDirectory) throws IOException {
        // Ensure the output directory exists
        ensureDirectoryExists(outputDirectory);
        
        // Always create a unique file with timestamp
        File destFile = createUniqueFile(file, outputDirectory);
        
        // Move the file
        log.info("Moving file {} to {}", file.getAbsolutePath(), destFile.getAbsolutePath());
        Files.move(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        
        return destFile;
    }
    
    /**
     * Moves a file to the failed directory.
     * 
     * @param file the file to move
     * @param failedDirectory the directory to move failed files to
     * @return the moved file
     * @throws IOException if there is an error moving the file
     */
    public File moveToFailed(File file, String failedDirectory) throws IOException {
        // Ensure the failed directory exists
        ensureDirectoryExists(failedDirectory);
        
        // Always create a unique file with timestamp
        File destFile = createUniqueFile(file, failedDirectory);
        
        // Move the file
        log.info("Moving failed file {} to {}", file.getAbsolutePath(), destFile.getAbsolutePath());
        Files.move(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        
        return destFile;
    }
    
    /**
     * Generic method to move a file to a target directory.
     * 
     * @param file the file to move
     * @param targetDirectory the directory to move the file to
     * @param isFailedFile whether this is a failed file (for logging purposes)
     * @return the moved file
     * @throws IOException if there is an error moving the file
     */
    public File moveFile(File file, String targetDirectory, boolean isFailedFile) throws IOException {
        // Ensure the target directory exists
        ensureDirectoryExists(targetDirectory);
        
        // Always create a unique file with timestamp
        File destFile = createUniqueFile(file, targetDirectory);
        
        // Move the file
        log.info("Moving {} file {} to {}", 
                isFailedFile ? "failed" : "processed", 
                file.getAbsolutePath(), 
                destFile.getAbsolutePath());
        Files.move(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        
        return destFile;
    }
    
    /**
     * Creates a unique file name by appending a timestamp if the file already exists.
     * 
     * @param file the original file
     * @param targetDirectory the directory where the file will be placed
     * @return a File object with a unique name
     */
    private File createUniqueFile(File file, String targetDirectory) {
        String baseName = FilenameUtils.getBaseName(file.getName());
        String extension = FilenameUtils.getExtension(file.getName());
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String newFileName = baseName + "." + timestamp + (extension.isEmpty() ? "" : "." + extension);
        return new File(targetDirectory, newFileName);
    }
}
