package com.practice.first_project.handlers;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.List;

/**
 * One place that turns exceptions into HTTP responses, for every controller.
 * The parent class (ResponseEntityExceptionHandler) already handles Spring MVC's own exceptions
 * (bad JSON, wrong HTTP method, missing parameter...) with the right 4xx status.
 *
 * Nobody calls these methods: when a controller (or the service it calls) throws, Spring MVC looks for the
 * @ExceptionHandler matching the exception type and uses its return value as the response.
 *   UserNotFoundException      thrown by UserService / PostService            -> 404
 *   PostNotFoundException      thrown by PostService.getPost()                -> 404 (comment-service's Feign client sees it as FeignException.NotFound)
 *   PostLimitExceededException thrown by PostService.createPost(s)()          -> 409
 *   UsernameTakenException     thrown by AuthService.register()               -> 409
 *   AuthenticationException    thrown by AuthService.login() (wrong password) -> 401
 *   AccessDeniedException      thrown by @PreAuthorize                        -> 403
 *   @Valid failures            DTOs in the dto package                        -> 400
 * Errors in the security filters (no/bad token) never reach here: see JsonSecurityErrorHandlers.
 */
@RestControllerAdvice
public class ErrorResponseHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ErrorResponseHandler.class);

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleUserNotFoundException(UserNotFoundException exception, WebRequest webRequest){
        ErrorDetails errorDetails = new ErrorDetails(exception.getMessage(), webRequest.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ErrorDetails> handlePostNotFound(PostNotFoundException exception, WebRequest webRequest){
        ErrorDetails errorDetails = new ErrorDetails(exception.getMessage(), webRequest.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
    }

    // 409 Conflict: the request is valid, but clashes with the current state (the user is at the post limit)
    @ExceptionHandler(PostLimitExceededException.class)
    public ResponseEntity<ErrorDetails> handlePostLimitExceeded(PostLimitExceededException exception, WebRequest webRequest){
        ErrorDetails errorDetails = new ErrorDetails(exception.getMessage(), webRequest.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UsernameTakenException.class)
    public ResponseEntity<ErrorDetails> handleUsernameTaken(UsernameTakenException exception, WebRequest webRequest){
        ErrorDetails errorDetails = new ErrorDetails(exception.getMessage(), webRequest.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.CONFLICT);
    }

    // Wrong login. Same message for unknown user and wrong password, on purpose.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorDetails> handleAuthentication(AuthenticationException exception, WebRequest webRequest){
        ErrorDetails errorDetails = new ErrorDetails("Invalid username or password", webRequest.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.UNAUTHORIZED);
    }

    // @PreAuthorize failures are thrown inside the controller call, so they land HERE, not in the
    // security filter's AccessDeniedHandler. Without this, the catch-all below would turn them into 500s.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDetails> handleAccessDenied(AccessDeniedException exception, WebRequest webRequest){
        ErrorDetails errorDetails = new ErrorDetails("You do not have permission for this action",
                webRequest.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.FORBIDDEN);
    }

    // Catch-all: log the real cause for developers, send a generic message to the client
    // (exception messages can leak SQL, class names or other internals).
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleAllExceptions(Exception exception, WebRequest webRequest){
        log.error("Unhandled error on {}", webRequest.getDescription(false), exception);
        ErrorDetails errorDetails = new ErrorDetails("Something went wrong. Please try again later.",
                webRequest.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // @Valid failed: return every field error, not just the first one
    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), "Validation failed",
                request.getDescription(false), errors);
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    // Method validation failed, e.g. an invalid item inside List<@Valid PostRequest>: same error shape as above
    @Override
    protected @Nullable ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<String> errors = ex.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .toList();
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), "Validation failed",
                request.getDescription(false), errors);
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }
}
