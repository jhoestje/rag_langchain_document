package com.johoco.springbatchpgaiapp.model;

/**
 * Enum representing the possible status values for a document.
 */
public enum DocumentStatus {
    NEW("NEW"),
    PROCESSED("PROCESSED"),
    FAILED("FAILED");
    
    private final String value;
    
    DocumentStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
    
    /**
     * Convert a string value to the corresponding enum value.
     * 
     * @param value the string value to convert
     * @return the corresponding enum value, or null if no match is found
     */
    public static DocumentStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        
        for (DocumentStatus status : DocumentStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        
        return null;
    }
}
