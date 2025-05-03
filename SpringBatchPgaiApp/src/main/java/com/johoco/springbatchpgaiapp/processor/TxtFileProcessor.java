package com.johoco.springbatchpgaiapp.processor;

import com.johoco.springbatchpgaiapp.model.FileExtension;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Processor for TXT files.
 */
@Slf4j
@Service
public class TxtFileProcessor implements FileProcessor {

    @Override
    public boolean canProcess(File file) {
        if (file == null) {
            return false;
        }
        return FileExtension.TXT == FileExtension.fromFile(file);
    }

    @Override
    public String extractContent(File file) throws IOException {
        if (file == null) {
            log.error("Cannot read content from null file");
            throw new IllegalArgumentException("File cannot be null");
        }
        
        try {
            log.debug("Reading content from TXT file: {}", file.getName());
            String content = Files.readString(file.toPath());
            log.debug("Successfully read {} characters from TXT file: {}", content.length(), file.getName());
            return content;
        } catch (IOException e) {
            log.error("Error reading TXT file {}: {}", file.getName(), e.getMessage(), e);
            throw e;
        }
    }
}
