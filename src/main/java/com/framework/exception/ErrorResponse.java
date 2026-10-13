package com.framework.exception;

/**
 * Standard error response body.
 * Frontend checks e.data?.error for "Expired" or "Unauthorized".
 */
public class ErrorResponse {

    private String error;
    private String message;

    public ErrorResponse() {}

    public ErrorResponse(String error) {
        this.error = error;
    }

    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
