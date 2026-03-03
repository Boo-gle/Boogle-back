package com.boogle.boogle.user.application

import com.boogle.boogle.global.error.BoogleException
import com.boogle.boogle.global.jwt.JwtProvider
import com.boogle.boogle.user.api.dto.LoginResponse
import com.boogle.boogle.user.domain.User
import com.boogle.boogle.user.infra.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.annotation.Propagation

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
    private val discordNotificationService: DiscordNotificationService,
    private val failureService: UserLoginFailureService
) {

    private val accessLog = LoggerFactory.getLogger("ACCESS_LOG")
    private val errorLog = LoggerFactory.getLogger("ERROR_LOG")

    fun login(loginId: String, password: String): LoginResponse {

        println("login 함수 진입 - loginId: $loginId")

        // 아이디 체크
        val user = userRepository.findByLoginId(loginId) ?: run {
            accessLog.info("로그인 실패 - 존재하지 않는 ID: {}", loginId)
            throw BoogleException.InvalidRequestException("아이디가 일치하지 않습니다.")
        }

        // 관리자 권한 체크
        if (user.role != "ADMIN") {
            accessLog.info("로그인 실패 - 권한 부족 ID: {}", loginId)
            throw BoogleException.UnauthorizedException("관리자만 로그인 가능합니다.")
        }

        // 비밀번호 체크
        if(!passwordEncoder.matches(password, user.password)) {

            println("비밀번호 불일치 - failCount 증가 직전: ${user.failCount}")

            val currentFailCount = failureService.increaseFailCount(user.id!!)

            println("failCount 증가 후: ${user.failCount} ")

            if(currentFailCount >= 3){
                println("3회 이상 실패 분기 진입")
                errorLog.warn("🚨 [WARN] 관리자 로그인 3회 이상 실패 - ID: {}, 현재 횟수: {}", loginId, currentFailCount)
                discordNotificationService.sendLoginFailAlert(loginId, currentFailCount)
            } else {
                accessLog.info("로그인 실패 - ID: {}, 현재 실패 횟수: {}", loginId, currentFailCount)
            }
            throw BoogleException.InvalidRequestException("비밀번호가 일치하지 않습니다.")
        }

        // 로그인 성공 시 실패 횟수 초기화
        if(user.failCount > 0){
            failureService.resetFailCount(user.id!!)
        }

        val token = jwtProvider.createToken(user.loginId, user.role)

        return LoginResponse(
            accessToken = token,
            adminName = user.name,
        )
    }

}