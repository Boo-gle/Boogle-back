package com.boogle.boogle.user.application

import com.boogle.boogle.global.error.BoogleException
import com.boogle.boogle.global.jwt.JwtProvider
import com.boogle.boogle.user.api.dto.LoginResponse
import com.boogle.boogle.user.infra.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider
) {

    fun login(loginId: String, password: String): LoginResponse {

        val user = userRepository.findByLoginId(loginId)
            ?: throw BoogleException.InvalidRequestException("아이디가 존재하지 않습니다.")

        if(user.role != "ADMIN") {
            throw BoogleException.UnauthorizedException("관리자만 로그인 가능합니다.")
        }

        if(!passwordEncoder.matches(password, user.password)) {
            throw BoogleException.InvalidRequestException("비밀번호가 일치하지 않습니다.")
        }

        val token = jwtProvider.createToken(user.loginId, user.role)

        return LoginResponse(
            accessToken = token,
            adminName = user.name,
        )
    }

}