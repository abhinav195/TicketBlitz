package com.ticketblitz.event.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleAuthorizationDenied: Should return 403 Forbidden")
    void handleAuthorizationDenied() {
        // Fix: Pass AuthorizationDecision instead of null
        AuthorizationDeniedException ex = new AuthorizationDeniedException(
                "Access Denied",
                new AuthorizationDecision(false)
        );

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleAuthorizationDenied(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(403);
        assertThat(response.getBody().message()).isEqualTo("Access Denied");
        assertThat(response.getBody().errors()).isNull();
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("handleValidationErrors: Should return 400 with field errors")
    void handleValidationErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("event", "title", "Title is required");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleValidationErrors(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("Validation Failed");
        assertThat(response.getBody().errors()).containsKey("title");
        assertThat(response.getBody().errors().get("title")).isEqualTo("Title is required");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("handleConstraintViolation: Should return 400 with constraint errors")
    void handleConstraintViolation() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);

        when(violation.getPropertyPath()).thenReturn(path);
        when(path.toString()).thenReturn("createEvent.price");
        when(violation.getMessage()).thenReturn("must be greater than 0");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleConstraintViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("Constraint Violation");
        assertThat(response.getBody().errors()).containsKey("createEvent.price");
        assertThat(response.getBody().errors().get("createEvent.price")).isEqualTo("must be greater than 0");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("handleRuntimeException: Should return 500 Internal Server Error")
    void handleRuntimeException() {
        RuntimeException ex = new RuntimeException("Database connection failed");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleRuntimeException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().message()).isEqualTo("Database connection failed");
        assertThat(response.getBody().errors()).isNull();
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("ErrorResponse: Should create record with all fields")
    void errorResponseRecord() {
        GlobalExceptionHandler.ErrorResponse response = new GlobalExceptionHandler.ErrorResponse(
                404,
                "Not Found",
                null,
                java.time.LocalDateTime.now()
        );

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.message()).isEqualTo("Not Found");
        assertThat(response.errors()).isNull();
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("handleValidationErrors: Should handle multiple field errors")
    void handleValidationErrors_MultipleFields() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError titleError = new FieldError("event", "title", "Title is required");
        FieldError descError = new FieldError("event", "description", "Description is required");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(titleError, descError));

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleValidationErrors(ex);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).hasSize(2);
        assertThat(response.getBody().errors()).containsKeys("title", "description");
    }

    @Test
    @DisplayName("handleConstraintViolation: Should handle multiple violations")
    void handleConstraintViolation_MultipleViolations() {
        ConstraintViolation<?> violation1 = mock(ConstraintViolation.class);
        ConstraintViolation<?> violation2 = mock(ConstraintViolation.class);

        Path path1 = mock(Path.class);
        Path path2 = mock(Path.class);

        when(violation1.getPropertyPath()).thenReturn(path1);
        when(path1.toString()).thenReturn("createEvent.price");
        when(violation1.getMessage()).thenReturn("must be greater than 0");

        when(violation2.getPropertyPath()).thenReturn(path2);
        when(path2.toString()).thenReturn("createEvent.totalTickets");
        when(violation2.getMessage()).thenReturn("must be at least 1");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation1, violation2));

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleConstraintViolation(ex);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).hasSize(2);
        assertThat(response.getBody().errors()).containsKeys("createEvent.price", "createEvent.totalTickets");
    }
}
