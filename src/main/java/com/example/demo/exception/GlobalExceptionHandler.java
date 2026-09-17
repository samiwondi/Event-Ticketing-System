package com.example.demo.exception;

import com.example.demo.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> notFound(
    ResourceNotFoundException ex,
    HttpServletRequest req
  ) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
      ErrorResponse.of(404, "Not Found", ex.getMessage(), req.getRequestURI())
    );
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ErrorResponse> conflict(
    ConflictException ex,
    HttpServletRequest req
  ) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(
      ErrorResponse.of(409, "Conflict", ex.getMessage(), req.getRequestURI())
    );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> validation(
    MethodArgumentNotValidException ex,
    HttpServletRequest req
  ) {
    List<ErrorResponse.FieldError> errors = ex
      .getBindingResult()
      .getFieldErrors()
      .stream()
      .map(fe ->
        new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage())
      )
      .toList();
    var body = new ErrorResponse(
      java.time.Instant.now(),
      400,
      "Bad Request",
      "Validation failed",
      req.getRequestURI(),
      errors
    );
    return ResponseEntity.badRequest().body(body);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> illegal(
    IllegalArgumentException ex,
    HttpServletRequest req
  ) {
    return ResponseEntity.badRequest().body(
      ErrorResponse.of(400, "Bad Request", ex.getMessage(), req.getRequestURI())
    );
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> general(
    Exception ex,
    HttpServletRequest req
  ) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
      ErrorResponse.of(
        500,
        "Internal Server Error",
        ex.getMessage(),
        req.getRequestURI()
      )
    );
  }
}
