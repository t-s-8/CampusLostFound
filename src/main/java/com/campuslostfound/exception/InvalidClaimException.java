package com.campuslostfound.exception;

/**
 * Thrown when an invalid claim operation is performed
 * (e.g., claiming a non-existent item or a lost item report).
 */
public class InvalidClaimException extends CampusLostFoundException {

    public InvalidClaimException(String message) {
        super(message);
    }

    public InvalidClaimException(String message, Throwable cause) {
        super(message, cause);
    }
}
