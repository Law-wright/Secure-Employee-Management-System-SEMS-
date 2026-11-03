package edu.sems.service;

public class LeaveValidationException extends RuntimeException {

    private final String field;

    public LeaveValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
