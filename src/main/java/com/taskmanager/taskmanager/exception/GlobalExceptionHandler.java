package com.taskmanager.taskmanager.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.taskmanager.taskmanager.common.ApiError;

import ch.qos.logback.core.net.SocketConnector;


/*@RestControllerAdvice = watches ALL controllers. When any exception is thrown anywhere, Spring routes it here instead of returning the default ugly error page. */


@RestControllerAdvice // apply to all controllers globally
public class GlobalExceptionHandler {

    // not found -- 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(404, ex.getMessage()));
    }

    // bad request - 400
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError.of(400, ex.getMessage()));
    }

    // unauthorized - 401
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiError.of(401, ex.getMessage()));
    }

    // 400 validation error 
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationError(MethodArgumentNotValidException ex){
        Map<String,String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error->fieldErrors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError.of(400, "Validation failed", fieldErrors));
    }


    // 400 - wrong type in url
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex){
        String message = String.format("Parameter '%s' should be of type '%s'",ex.getName(),ex.getRequiredType()!=null ? ex.getRequiredType().getSimpleName():"unknown");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError.of(400, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAll(Exception ex){
        ex.printStackTrace();;
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError.of(500, "An unexpected error occurred. Please try again later."));
    }
}
