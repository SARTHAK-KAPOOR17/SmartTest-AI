package com.smarttestai.exception;

public class UserStoryNotReadyException extends RuntimeException {

    public UserStoryNotReadyException(String message) {
        super(message);
    }
}
