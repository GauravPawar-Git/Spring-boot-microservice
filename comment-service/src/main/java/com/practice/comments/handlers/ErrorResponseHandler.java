package com.practice.comments.handlers;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Turns exceptions thrown by CommentController / CommentService into JSON (ErrorDetails), like user-service's
 * handler of the same name. Spring MVC picks the method by exception type; nobody calls them directly.
 *   NotFoundException               CommentService (post or comment missing)         -> 404
 *   BannedWordException             CommentService.addComment() via CommentRules     -> 400
 *   UserServiceUnavailableException PostLookup.findPost() fallback                   -> 503
 *   AccessDeniedException           CommentService.deleteComment() (not the author)  -> 403
 *   @Valid failures                 CommentRequest                                   -> 400
 * Exceptions in the Kafka listener do NOT come here (no HTTP request); Spring Kafka logs and retries them.
 */
@RestControllerAdvice
public class ErrorResponseHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ErrorResponseHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorDetails> handleNotFound(NotFoundException exception, WebRequest request) {
        return new ResponseEntity<>(new ErrorDetails(exception.getMessage(), request.getDescription(false)), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BannedWordException.class)
    public ResponseEntity<ErrorDetails> handleBannedWord(BannedWordException exception, WebRequest request) {
        return new ResponseEntity<>(new ErrorDetails(exception.getMessage(), request.getDescription(false)), HttpStatus.BAD_REQUEST);
    }

    // 503: we're fine, a service we depend on isn't. Clients may retry later.
    @ExceptionHandler(UserServiceUnavailableException.class)
    public ResponseEntity<ErrorDetails> handleUnavailable(UserServiceUnavailableException exception, WebRequest request) {
        return new ResponseEntity<>(new ErrorDetails(exception.getMessage(), request.getDescription(false)), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDetails> handleAccessDenied(AccessDeniedException exception, WebRequest request) {
        return new ResponseEntity<>(new ErrorDetails("You do not have permission for this action",
                request.getDescription(false)), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleAll(Exception exception, WebRequest request) {
        log.error("Unhandled error on {}", request.getDescription(false), exception);
        return new ResponseEntity<>(new ErrorDetails("Something went wrong. Please try again later.",
                request.getDescription(false)), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                          HttpStatusCode status, WebRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return new ResponseEntity<>(new ErrorDetails(LocalDateTime.now(), "Validation failed",
                request.getDescription(false), errors), HttpStatus.BAD_REQUEST);
    }
}
