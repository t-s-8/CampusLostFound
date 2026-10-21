package com.campuslostfound.exception;

/**
 * Thrown when an item has invalid, missing, or contradictory attributes.
 */
public class InvalidItemException extends CampusLostFoundException {

    public InvalidItemException(String message) {
        super(message);
    }

    public InvalidItemException(String message, Throwable cause) {
        super(message, cause);
    }
}
