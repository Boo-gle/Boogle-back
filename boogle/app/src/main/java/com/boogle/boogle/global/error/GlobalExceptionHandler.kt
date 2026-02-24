package com.boogle.boogle.global.error

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler {

    // logback-spring.xml의 ERROR_LOG 로거 사용
    private val errorLogger = LoggerFactory.getLogger("ERROR_LOG")

    // 커스텀 비즈니스 예외처리
    @ExceptionHandler(BoogleException::class)
    fun handleBoogleException(e: BoogleException): ResponseEntity<ErrorResponse> {
        // 비즈니스 에러는 WARN 레벨로 기록하여 로그 노이즈를 줄인다
        // errorCode와 message를 명확히 남긴다
        errorLogger.warn("event=BUSINESS_ERROR code={} message={}", e.errorCode, e.message)

        return ResponseEntity
            .status(e.status)
            .body(ErrorResponse(e.errorCode, e.message, e.status.value()))
    }

    // 예상치 못한 서버 내부 에러
    @ExceptionHandler(Exception::class)
    fun handleAllException(e: Exception): ResponseEntity<ErrorResponse> {
        // 진짜 서버 에러는 StackTrace까지 포함하여 ERROR 레베로 기록
        errorLogger.error("event=SERVER_ERROR message={}", e.message, e)

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.", 500))
    }

    // 잘못된 요청
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(e: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        errorLogger.warn("event=BAD_REQUEST message={}", e.message)

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse("INVALID_INPUT", e.message ?: "잘못된 입력값입니다.", 400))
    }

    // 파비콘 같은 리소스 없을때 에러 warn
    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(e: NoResourceFoundException): ResponseEntity<ErrorResponse> {
        errorLogger.warn("event=RESOURCE_NOT_FOUND message={}", e.message)

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse("RESOURCE_NOT_FOUND", "요청하신 리소스를 찾을 수 없습니다.", 404))
    }

}

// 응답을 위한 공통 DTO
data class ErrorResponse(
    val code: String,
    val message: String,
    val status: Int, // JSON 응답용 숫자값
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun of(code: String, message: String, status: Int): ErrorResponse {
            return ErrorResponse(code, message, status)
        }
    }
}
