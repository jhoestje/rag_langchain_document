package com.johoco.springbatchpgaiapp.processor;

import java.io.File;
import java.io.IOException;

/**
 * Interface for file processors that handle different file types.
 */
public interface FileProcessor {
    
    /**
     * Checks if this processor can handle the given file based on its extension.
     * 
     * @param file the file to check
     * @return true if this processor can handle the file, false otherwise
     */
    boolean canProcess(File file);
    
    /**
     * Processes the file and extracts its content.
     * 
     * @param file the file to process
     * @return the content of the file as a string
     * @throws IOException if an I/O error occurs
     */
    String extractContent(File file) throws IOException;
}
