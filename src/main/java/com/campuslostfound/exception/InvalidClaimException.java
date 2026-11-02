package com.campuslostfound.exception;

public class InvalidClaimException extends CampusLostFoundException {

    public InvalidClaimException(String message) {
        super(message);
    }

    public InvalidClaimException(String message, Throwable cause) {
        super(message, cause);
    }
}
