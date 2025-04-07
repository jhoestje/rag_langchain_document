package com.johoco.springbatchpgaiapp.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
// @Service
public class FileManagementService {
    
    private String successDir;
    private String failureDir;
    
    // public FileManagementService(
    //         @Value("${document.success.directory}") String successDir,
    //         @Value("${document.failure.directory}") String failureDir) {
    //     this.successDir = successDir;
    //     this.failureDir = failureDir;
    //     log.info("FileManagementService constructed with success directory: {} and failure directory: {}", 
    //             successDir, failureDir);
    // }
    
    @PostConstruct
    public void init() {
        // Ensure directories exist
        createDirectoryIfNotExists(successDir);
        createDirectoryIfNotExists(failureDir);
        log.info("FileManagementService initialized with success directory: {} and failure directory: {}", 
                successDir, failureDir);
    }
    
    /**
     * Move a successfully processed file to the success directory
     * 
     * @param file The file to move
     * @return true if the move was successful, false otherwise
     */
    public boolean moveToSuccessDirectory(File file) {
        if (file == null || !file.exists()) {
            log.error("Cannot move null or non-existent file to success directory");
            return false;
        }
        
        return moveFile(file, successDir);
    }
    
    /**
     * Move a file that failed processing to the failure directory
     * 
     * @param file The file to move
     * @return true if the move was successful, false otherwise
     */
    public boolean moveToFailureDirectory(File file) {
        if (file == null || !file.exists()) {
            log.error("Cannot move null or non-existent file to failure directory");
            return false;
        }
        
        return moveFile(file, failureDir);
    }
    
    private boolean moveFile(File file, String targetDirectory) {
        try {
            Path source = file.toPath();
            Path target = Paths.get(targetDirectory, file.getName());
            
            // Create target directory if it doesn't exist
            Files.createDirectories(target.getParent());
            
            // Copy the file to the target directory
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            
            // Delete the source file after successful copy
            boolean deleted = Files.deleteIfExists(source);
            
            if (!deleted) {
                // If delete fails, try with File API as fallback
                deleted = file.delete();
                if (!deleted) {
                    log.warn("Failed to delete source file {} after copying to {}", file.getName(), targetDirectory);
                    // Schedule deletion on JVM exit as last resort
                    file.deleteOnExit();
                }
            }
            
            log.info("Successfully moved file {} to {}", file.getName(), targetDirectory);
            return true;
        } catch (IOException e) {
            log.error("Failed to move file {} to {}: {}", file.getName(), targetDirectory, e.getMessage(), e);
            return false;
        }
    }
    
    private void createDirectoryIfNotExists(String directory) {
        Path path = Paths.get(directory);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
                log.info("Created directory: {}", directory);
            } catch (IOException e) {
                log.error("Failed to create directory {}: {}", directory, e.getMessage(), e);
            }
        }
    }
}
