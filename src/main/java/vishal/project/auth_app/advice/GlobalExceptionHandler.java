package vishal.project.auth_app.advice;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.tomcat.websocket.AuthenticationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vishal.project.auth_app.exception.BadRequestException;
import vishal.project.auth_app.exception.ResourceNotFoundException;
import vishal.project.auth_app.exception.UnauthorizedException;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<?>> handleBadRequest(BadRequestException exception, HttpServletRequest request) {
        return buildErrorResponseEntity(
                HttpStatus.BAD_REQUEST,
                "BAD_REQUEST",
                exception.getMessage(),
                null,
                request
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleResourceNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return buildErrorResponseEntity(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                exception.getMessage(),
                null,
                request
        );
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<?>> handleUnauthorizedException(UnauthorizedException exception, HttpServletRequest request) {
        return buildErrorResponseEntity(
                HttpStatus.UNAUTHORIZED,
                "AUTH_UNAUTHORIZED",
                exception.getMessage(),
                null,
                request
        );
    }

    /** Covers Spring Security's AuthenticationManager bad password/username */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<?>> handleBadCredentials(BadCredentialsException exception, HttpServletRequest request) {
        return buildErrorResponseEntity(
                HttpStatus.UNAUTHORIZED,
                "AUTH_BAD_CREDENTIALS",
                "Bad credentials",
                null,
                request
        );
    }

    @ExceptionHandler(SessionAuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleSessionAuthentication(SessionAuthenticationException exception, HttpServletRequest request) {
        return buildErrorResponseEntity(
                HttpStatus.UNAUTHORIZED,
                "AUTH_INVALID_SESSION",
                exception.getMessage(),
                null,
                request
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthenticationException(AuthenticationException exception, HttpServletRequest request) {
        return buildErrorResponseEntity(
                HttpStatus.UNAUTHORIZED,
                "AUTH_UNAUTHORIZED",
                exception.getMessage(),
                null,
                request
        );
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiResponse<?>> handleJwtException(JwtException exception, HttpServletRequest request) {
        return buildErrorResponseEntity(
                HttpStatus.UNAUTHORIZED,
                "AUTH_INVALID_TOKEN",
                exception.getMessage(),
                null,
                request
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDeniedException(AccessDeniedException exception, HttpServletRequest request) {
        return buildErrorResponseEntity(
                HttpStatus.FORBIDDEN,
                "AUTH_FORBIDDEN",
                exception.getMessage(),
                null,
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleInputValidationErrors(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<String> errors = exception
                .getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.toList());

        return buildErrorResponseEntity(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Input validation failed",
                errors,
                request
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<?>> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
        return buildErrorResponseEntity(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "UNSUPPORTED_MEDIA_TYPE",
                "Unsupported Content-Type. Please send 'Content-Type: application/json'.",
                List.of(exception.getMessage()),
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleInternalServerError(Exception exception, HttpServletRequest request) {
        return buildErrorResponseEntity(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                exception.getMessage(),
                null,
                request
        );
    }

    private ResponseEntity<ApiResponse<?>> buildErrorResponseEntity(
            HttpStatus status,
            String code,
            String message,
            List<String> details,
            HttpServletRequest request
    ) {
        ApiError apiError = ApiError.builder()
                .code(code)
                .message(message)
                .details(details)
                .build();

        String path = request != null ? request.getRequestURI() : null;

        ApiResponse<?> apiResponse = ApiResponse.failure(apiError, status.value(), path);
        return new ResponseEntity<>(apiResponse, status);
    }

}
