package com.example.orderservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformedRequest(
            HttpMessageNotReadableException ex
    ) {

        log.warn("Malformed request body", ex);

        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

        problem.setTitle("Malformed Request");
        problem.setDetail("Request body could not be read.");

        return problem;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleInvalidRequestParameter(
            MethodArgumentTypeMismatchException ex
    ) {

        log.warn("Invalid request parameter {}", ex.getName());

        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

        problem.setTitle("Invalid Request");
        problem.setDetail("Request parameter " + ex.getName() + " is invalid.");

        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(
            MethodArgumentNotValidException ex
    ) {

        log.warn("Validation failed");

        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

        problem.setTitle("Validation Failed");
        problem.setDetail("Request validation failed.");

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        problem.setProperty("errors", errors);

        return problem;
    }

    @ExceptionHandler(PartNotFoundException.class)
    public ProblemDetail handlePartNotFound(
            PartNotFoundException ex
    ) {

        log.warn("Part not found: {}", ex.getMessage());

        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND);

        problem.setTitle("Part Not Found");
        problem.setDetail(ex.getMessage());

        return problem;
    }

    @ExceptionHandler(CatalogueUnavailableException.class)
    public ProblemDetail handleCatalogueUnavailable(
            CatalogueUnavailableException ex
    ) {

        log.warn("Catalogue unavailable: {}", ex.getMessage());

        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);

        problem.setTitle("Catalogue Service Unavailable");
        problem.setDetail(ex.getMessage());

        return problem;
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail handleOrderNotFound(
            OrderNotFoundException ex
    ) {

        log.warn("Order not found: {}", ex.getMessage());

        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND);

        problem.setTitle("Order Not Found");
        problem.setDetail(ex.getMessage());

        return problem;
    }

    @ExceptionHandler(InvalidOrderStatusTransitionException.class)
    public ProblemDetail handleInvalidTransition(
            InvalidOrderStatusTransitionException ex
    ) {

        log.warn("Invalid order status transition: {}", ex.getMessage());

        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.CONFLICT);

        problem.setTitle("Invalid Order Status Transition");
        problem.setDetail(ex.getMessage());

        return problem;
    }

    @ExceptionHandler(OutboxEventNotFoundException.class)
    public ProblemDetail handleOutboxEventNotFound(
            OutboxEventNotFoundException ex
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Outbox Event Not Found");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception ex) {

        log.error("Unexpected server error", ex);

        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);

        problem.setTitle("Internal Server Error");
        problem.setDetail("An unexpected error occurred.");

        return problem;
    }


}
