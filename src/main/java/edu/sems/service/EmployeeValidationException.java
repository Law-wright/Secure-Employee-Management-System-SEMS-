package edu.sems.service;

public class EmployeeValidationException extends RuntimeException {

    private final String field;

    public EmployeeValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
