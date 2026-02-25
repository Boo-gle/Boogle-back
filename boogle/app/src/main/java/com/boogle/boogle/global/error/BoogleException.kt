package com.boogle.boogle.global.error

import org.springframework.http.HttpStatus

sealed class BoogleException (
    override val message: String,
    val errorCode: String,
    val status: HttpStatus
) : RuntimeException(message) {
    // 책을 찾을 수 없을 때
    class BookNotFoundException (message: String = "해당 책을 찾을 수 없습니다.") :
        BoogleException(message, "BOOK_NOT_FOUND", HttpStatus.NOT_FOUND)

    // 권한 없을 때
    class UnauthorizedException(message: String = "로그인이 필요합니다.") :
            BoogleException(message, "UNAUTHORIZED", HttpStatus.UNAUTHORIZED)

    // 나중에 추가될 수 있는 예외 (ex: 입력값 오류)
    class InvalidRequestException(message: String) :
            BoogleException(message, "INVALID_REQUEST", HttpStatus.BAD_REQUEST)
}

