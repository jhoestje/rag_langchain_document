package com.johoco.springbatchpgaiapp.processor;

import com.johoco.springbatchpgaiapp.model.FileExtension;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

/**
 * Factory for selecting the appropriate file processor based on file type.
 */
@Slf4j
@Service
public class FileProcessorFactory {

    private final List<FileProcessor> processors;

    public FileProcessorFactory(List<FileProcessor> processors) {
        this.processors = processors;
        log.info("FileProcessorFactory initialized with {} processors", processors.size());
        processors.forEach(p -> log.debug("Registered processor: {}", p.getClass().getSimpleName()));
    }

    /**
     * Gets the appropriate processor for the given file.
     *
     * @param file the file to process
     * @return the appropriate processor, or null if no suitable processor is found
     */
    public FileProcessor getProcessorForFile(File file) {
        if (file == null) {
            log.error("Cannot get processor for null file");
            return null;
        }

        for (FileProcessor processor : processors) {
            if (processor.canProcess(file)) {
                FileExtension extension = FileExtension.fromFile(file);
        log.debug("Using processor {} for file {} with extension {}", 
                processor.getClass().getSimpleName(), 
                file.getName(),
                extension.getValue());
                return processor;
            }
        }

        FileExtension extension = FileExtension.fromFile(file);
        log.warn("No suitable processor found for file: {} with extension: {}", 
                file.getName(), 
                extension.getValue());
        return null;
    }
}
