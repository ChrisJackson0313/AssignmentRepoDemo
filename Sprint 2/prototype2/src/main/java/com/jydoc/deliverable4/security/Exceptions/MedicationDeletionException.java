package com.jydoc.deliverable4.security.Exceptions;

public class MedicationDeletionException extends RuntimeException {
    public MedicationDeletionException(String message, Throwable cause) {
        super(message, cause);
    }
}