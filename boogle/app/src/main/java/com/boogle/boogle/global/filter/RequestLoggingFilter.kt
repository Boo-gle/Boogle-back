package com.boogle.boogle.global.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class RequestLoggingFilter : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger("ACCESS_LOG")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val startTime = System.currentTimeMillis()
        val requestId = UUID.randomUUID().toString()
        val userId = request.getHeader("X-USER-ID") ?: "GUEST"

        MDC.put("requestId", requestId)
        MDC.put("userId", userId)
        MDC.put("method", request.method)
        MDC.put("uri", request.requestURI)
        MDC.put("ip", request.remoteAddr)

        try {
            filterChain.doFilter(request, response)
        } finally {
            val duration = System.currentTimeMillis() - startTime

            logger.info(
                "event=ACCESS method={} uri={} status={} durationMs={}",
                request.method,
                request.requestURI,
                response.status,
                duration
            )

            // 응답 속도가 느린 경우 경로 로그 남기기
            if (duration > 1000) {
                logger.warn("event=SLOW_API durationMs={} threshold=1000", duration)
            }

            MDC.clear()
        }
    }

}