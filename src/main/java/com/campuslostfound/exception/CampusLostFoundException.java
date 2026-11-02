package com.campuslostfound.exception;

public class CampusLostFoundException extends RuntimeException {

    public CampusLostFoundException(String message) {
        super(message);
    }

    public CampusLostFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
