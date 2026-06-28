package cc.hrva.ladica.configuration;

import cc.hrva.ladica.exception.InvalidTokenException;
import cc.hrva.ladica.exception.LinkLimitExceededException;
import cc.hrva.ladica.exception.NotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_KEY = "error";
    private static final String GENERIC_ERROR_MESSAGE = "Internal server error";
    private static final String DEFAULT_BAD_REQUEST_MESSAGE = "Bad request";
    private static final String DEFAULT_VALIDATION_MESSAGE = "Invalid value";
    private static final String MALFORMED_REQUEST_MESSAGE = "Malformed request body";
    private static final String CONFLICT_MESSAGE = "Resource conflict";

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(final NotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(ERROR_KEY, safeMessage(exception)));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidToken(final InvalidTokenException exception) {
        return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, safeMessage(exception)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleMalformedBody(final HttpMessageNotReadableException exception) {
        log.debug("Rejected malformed request body", exception);

        return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, MALFORMED_REQUEST_MESSAGE));
    }

    @ExceptionHandler(LinkLimitExceededException.class)
    public ResponseEntity<Map<String, String>> handleLinkLimitExceeded(final LinkLimitExceededException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(ERROR_KEY, safeMessage(exception)));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(final DataIntegrityViolationException exception) {
        log.warn("Rejected request due to data integrity violation", exception);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(ERROR_KEY, CONFLICT_MESSAGE));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(final MethodArgumentNotValidException exception) {
        final var fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> defaultValidationMessage(fieldError.getDefaultMessage()),
                        (firstMessage, secondMessage) -> secondMessage));

        return ResponseEntity.badRequest().body(fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(final ConstraintViolationException exception) {
        final var violations = exception.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> defaultValidationMessage(violation.getMessage()),
                        (firstMessage, secondMessage) -> secondMessage));

        return ResponseEntity.badRequest().body(violations);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(final Exception exception) {
        log.error("Unhandled exception", exception);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(ERROR_KEY, GENERIC_ERROR_MESSAGE));
    }

    private static String safeMessage(final Exception exception) {
        return StringUtils.defaultIfBlank(exception.getMessage(), DEFAULT_BAD_REQUEST_MESSAGE);
    }

    private static String defaultValidationMessage(final String message) {
        return StringUtils.defaultIfBlank(message, DEFAULT_VALIDATION_MESSAGE);
    }

}
