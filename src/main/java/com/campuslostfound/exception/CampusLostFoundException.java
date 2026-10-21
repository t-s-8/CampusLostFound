package com.campuslostfound.exception;

/**
 * Base unchecked exception for all Campus Lost and Found domain and processing errors.
 */
public class CampusLostFoundException extends RuntimeException {

    public CampusLostFoundException(String message) {
        super(message);
    }

    public CampusLostFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
