package com.boogle.boogle.user.application

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class DiscordNotificationService {

    private val errorLogger = LoggerFactory.getLogger("ERROR_LOG")

    private val restTemplate = RestTemplate()

    private val webhookUrl = "https://discord.com/api/webhooks/1478310578899648645/uxXLnSwg7e1pQlsDRkAgzdqEXkfWkJpXN70ps2S7HOd-wNeMut4Q_YtYIlgJh95-eXT5"

    @Async  // 알림 보내느라 로그인 응답이 늦어지지 않게 비동기 처리
    fun sendLoginFailAlert(loginId: String, failCount: Int) {
        val body = mapOf(
            "content" to """
                🚨 **[Boogle 보안 경고] 관리자 로그인 실패**
                - **계정 ID**: $loginId
                - **실패 횟수**: ${failCount}회
                - **확인**: 즉시 계정 상태를 확인하세요.
            """.trimIndent()
        )
        try {
            restTemplate.postForEntity(webhookUrl, body, String::class.java)
        } catch (e: Exception) {
            errorLogger.error("디스코드 알림 전송 중 에러 발생 : {}", e.message, e)
        }
    }

}