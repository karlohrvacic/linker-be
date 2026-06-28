package cc.hrva.ladica.exception;

public class LinkLimitExceededException extends RuntimeException {

    public LinkLimitExceededException(final String message) {
        super(message);
    }

}
