package com.campuslostfound.exception;

public class InvalidItemException extends CampusLostFoundException {

    public InvalidItemException(String message) {
        super(message);
    }

    public InvalidItemException(String message, Throwable cause) {
        super(message, cause);
    }
}
