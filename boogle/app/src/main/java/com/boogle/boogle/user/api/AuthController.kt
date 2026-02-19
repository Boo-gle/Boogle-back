package com.boogle.boogle.user.api

import com.boogle.boogle.user.api.dto.LoginRequest
import com.boogle.boogle.user.application.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin")
class AuthController (
    private val userService: UserService
){

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<String> {
        val token = userService.login(request.loginId, request.password)

        return ResponseEntity.ok(token)
    }

}