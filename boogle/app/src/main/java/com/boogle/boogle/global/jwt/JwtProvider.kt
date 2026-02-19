package com.boogle.boogle.global.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.security.Key
import java.util.*

@Component
class JwtProvider {

    private val secretKey: Key = Keys.hmacShaKeyFor(
        "your-very-long-secret-key-should-be-at-least-32-characters".toByteArray()
    )

    // 만료시간 설정
    private val expirationTime = 1000 * 60 *60  // 1시간

    // 토큰 생성
    fun createToken(loginId: String, role: String): String {
        // 현재 시간 생성
        val now = Date()

        // 만료 시간 계산
        val expiry = Date(now.time + expirationTime)

        // JWT 빌드
        return Jwts.builder()
            // Subject 설정 (해당 토큰의 주인 = loginId, 나중에 claims.subject로 꺼낼 수 있음)
            .setSubject(loginId)
            // role 추가 (Payload 안에 role 저장)
            .claim("role", role)
            // 발급시간 & 만료시간
            .setIssuedAt(now)
            .setExpiration(expiry)
            // 서명 (위조방지, secretKey 없으면 조작 못 함)
            .signWith(secretKey, SignatureAlgorithm.HS256)
            // 최종 문자열 생성
            .compact()
    }

    // Jwt 토큰 안에 들어있는 정보 꺼내는 로직
    fun getClaims(token: String): Claims {
        // JWT 문자열을 받아서 Claims 객체로 반환

        // parser 생성
        return Jwts.parserBuilder()
            // 서명 키 설정 (토큰 생성시 썼던 secretKey로 토큰 위조, 서명 맞는지 검증)
            .setSigningKey(secretKey)
            .build()
            // 파싱 실행 (토큰 구조 검사, 서명 검증, 만료 시간 확인 / 이 단계에서 문제 있으면 Exception 터짐)
            .parseClaimsJws(token)
            // body 반환 (이 부분이 JWT Payload 부분)
            .body
    }

    // 유효성 검사
    fun validateToken(token: String): Boolean {
        return try {
            // 서명 검증, 만료시간 체크, 위조 여부 검사, 토큰 형식 검새 전부 수행
            getClaims(token)
            // 예외가 안 터지면 true
            true
        } catch (e: Exception) {
            // 예외 터지면 false
            false
        }
    }
}