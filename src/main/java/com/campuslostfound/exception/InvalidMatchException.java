package com.campuslostfound.exception;

public class InvalidMatchException extends CampusLostFoundException {

    public InvalidMatchException(String message) {
        super(message);
    }

    public InvalidMatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
