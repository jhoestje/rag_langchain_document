package com.johoco.springbatchpgaiapp.processor;

import com.johoco.springbatchpgaiapp.model.FileExtension;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

/**
 * Processor for PDF files.
 */
@Slf4j
@Service
public class PdfFileProcessor implements FileProcessor {

    @Override
    public boolean canProcess(File file) {
        if (file == null) {
            return false;
        }
        return FileExtension.PDF == FileExtension.fromFile(file);
    }

    @Override
    public String extractContent(File file) throws IOException {
        if (file == null) {
            log.error("Cannot read content from null file");
            throw new IllegalArgumentException("File cannot be null");
        }
        
        try (PDDocument document = PDDocument.load(file)) {
            log.debug("Reading content from PDF file: {}", file.getName());
            PDFTextStripper stripper = new PDFTextStripper();
            String content = stripper.getText(document);
            log.debug("Successfully read {} characters from PDF file: {}", content.length(), file.getName());
            return content;
        } catch (IOException e) {
            log.error("Error reading PDF file {}: {}", file.getName(), e.getMessage(), e);
            throw e;
        }
    }
}
