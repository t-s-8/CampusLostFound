package com.campuslostfound.exception;

/**
 * Thrown when an illegal match comparison is attempted
 * (e.g. comparing the same item, or comparing two items of the same type).
 */
public class InvalidMatchException extends CampusLostFoundException {

    public InvalidMatchException(String message) {
        super(message);
    }

    public InvalidMatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
