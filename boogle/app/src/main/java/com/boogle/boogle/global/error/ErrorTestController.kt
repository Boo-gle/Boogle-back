package com.boogle.boogle.global.error

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ErrorTestController {

    // 커스텀 비즈니스 예외 테스트 warn 로그
    @GetMapping("/api/test/business-error")
    fun testBusinessError(){
        throw BoogleException.BookNotFoundException()
    }

    // 예상치 못한 서버 에러 테스트 ERROR 로그 + StackTrace
    @GetMapping("/api/test/server-error")
    fun testServerError(){
        throw RuntimeException("로깅 시스템 테스트를 위한 강제 서버 에러!")
    }

    // 잘못된 요청 에러 테스트 BAD_REQUEST 로그
    @GetMapping("/api/test/bad-request")
    fun testBadRequest(){
        throw IllegalArgumentException("잘못된 파라미터가 전달되었습니다.")
    }

}