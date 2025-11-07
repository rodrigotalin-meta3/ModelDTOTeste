package br.com.meta3.java.scaffold.application.exceptions;

/**
 * Unchecked exception to wrap SQL-related errors during database operations.
 */
// TODO: (REVIEW) Use this exception to wrap any SQLException and provide clearer context for DB failures
public class DataBaseOperationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new DataBaseOperationException with the specified detail message.
     *
     * @param message the detail message
     */
    public DataBaseOperationException(String message) {
        super(message);
    }

    /**
     * Constructs a new DataBaseOperationException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the root cause (usually a SQLException)
     */
    public DataBaseOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
