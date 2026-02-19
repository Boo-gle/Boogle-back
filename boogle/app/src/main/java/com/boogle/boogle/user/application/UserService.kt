package com.boogle.boogle.user.application

import com.boogle.boogle.global.jwt.JwtProvider
import com.boogle.boogle.user.infra.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider
) {

    fun login(loginId: String, password: String): String {

        val user = userRepository.findByLoginId(loginId)
            ?: throw RuntimeException("아이디가 존재하지 않습니다.")

        if(user.role != "admin") {
            throw RuntimeException("관리자만 로그인 가능합니다.")
        }

        if(!passwordEncoder.matches(password, user.password)) {
            throw RuntimeException("비밀번호가 일치하지 않습니다.")
        }

        return jwtProvider.createToken(user.loginId, user.role)
    }

}