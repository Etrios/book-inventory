package com.ganeshl.bookinventory.api.exception

import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

data class ErrorResponse(val timestamp: Long, val status: Int, val error: String, val message: String?, val path: String)

class BookNotFoundException(message: String) : RuntimeException(message)
class DuplicateIsbnException(message: String) : RuntimeException(message)


@ControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(BookNotFoundException::class)
    fun handleBookNotFoundException(ex: BookNotFoundException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("BookNotFoundException: ${ex.message}")
        val errorDetails = ErrorResponse(
            System.currentTimeMillis(),
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.message,
            request.getDescription(false).substringAfter("uri=")
        )
        return ResponseEntity(errorDetails, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(DuplicateIsbnException::class)
    fun handleDuplicateIsbnException(ex: DuplicateIsbnException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("DuplicateIsbnException: ${ex.message}")
        val errorDetails = ErrorResponse(
            System.currentTimeMillis(),
            HttpStatus.CONFLICT.value(),
            "Conflict",
            ex.message,
            request.getDescription(false).substringAfter("uri=")
        )
        return ResponseEntity(errorDetails, HttpStatus.CONFLICT)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("IllegalArgumentException: ${ex.message}")
        val errorDetails = ErrorResponse(
            System.currentTimeMillis(),
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.message,
            request.getDescription(false).substringAfter("uri=")
        )
        return ResponseEntity(errorDetails, HttpStatus.BAD_REQUEST)
    }


    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException, request: WebRequest): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        logger.warn("ValidationException: $errors")
        val errorDetails = ErrorResponse(
            System.currentTimeMillis(),
            HttpStatus.BAD_REQUEST.value(),
            "Validation Error",
            errors,
            request.getDescription(false).substringAfter("uri=")
        )
        return ResponseEntity(errorDetails, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(ex: MethodArgumentTypeMismatchException, request: WebRequest): ResponseEntity<ErrorResponse> {
        val errors = "${ex.parameter}: ${ex.message}"
        logger.warn("ConstraintViolationException: $errors")
        val errorDetails = ErrorResponse(
            System.currentTimeMillis(),
            HttpStatus.BAD_REQUEST.value(),
            "Validation Error",
            errors,
            request.getDescription(false).substringAfter("uri=")
        )
        return ResponseEntity(errorDetails, HttpStatus.BAD_REQUEST)
    }

//    @ExceptionHandler(ConstraintViolationException::class) // For @Validated on path variables/request params
//    fun handleConstraintViolationException(ex: ConstraintViolationException, request: WebRequest): ResponseEntity<ErrorResponse> {
//        val errors = ex.constraintViolations.joinToString(", ") { "${it.propertyPath}: ${it.message}" }
//        logger.warn("ConstraintViolationException: $errors")
//        val errorDetails = ErrorResponse(
//            System.currentTimeMillis(),
//            HttpStatus.BAD_REQUEST.value(),
//            "Validation Error",
//            errors,
//            request.getDescription(false).substringAfter("uri=")
//        )
//        return ResponseEntity(errorDetails, HttpStatus.BAD_REQUEST)
//    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(ex: AccessDeniedException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("AccessDeniedException: ${ex.message} for path ${request.getDescription(false)}")
        val errorDetails = ErrorResponse(
            System.currentTimeMillis(),
            HttpStatus.FORBIDDEN.value(),
            "Forbidden",
            "You do not have permission to access this resource.",
            request.getDescription(false).substringAfter("uri=")
        )
        return ResponseEntity(errorDetails, HttpStatus.FORBIDDEN)
    }

    @ExceptionHandler(Exception::class)
    fun handleGlobalException(ex: Exception, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.error("Unhandled exception: ${ex.message}", ex)
        val errorDetails = ErrorResponse(
            System.currentTimeMillis(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An unexpected error occurred. Please try again later.",
            request.getDescription(false).substringAfter("uri=")
        )
        return ResponseEntity(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}