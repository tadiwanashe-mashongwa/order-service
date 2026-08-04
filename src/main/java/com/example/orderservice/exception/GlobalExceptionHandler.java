package com.example.orderservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PartNotFoundException.class)
    public ProblemDetail handlePartNotFound(PartNotFoundException ex) {

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Part Not Found");
        problem.setDetail(ex.getMessage());

        return problem;
    }

    @ExceptionHandler(CatalogueUnavailableException.class)
    public ProblemDetail handleCatalogueUnavailable(CatalogueUnavailableException ex) {

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        problem.setTitle("Catalogue Service Unavailable");
        problem.setDetail(ex.getMessage());

        return problem;
    }

}