package com.boogle.boogle.global.jwt

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter (

    private val jwtProvider: JwtProvider

) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {

        val header = request.getHeader("Authorization")

        if (header != null && header.startsWith("Bearer ")) {
            val token = header.substring(7)

            if(jwtProvider.validateToken(token)) {

                val claims = jwtProvider.getClaims(token)
                val loginId = claims.subject
                val role = claims["role"] as String

                val authorities = listOf(SimpleGrantedAuthority("ROLE_$role"))

                val authentication = UsernamePasswordAuthenticationToken(loginId, null, authorities)

                SecurityContextHolder.getContext().authentication = authentication

            }
        }

        filterChain.doFilter(request, response)

    }

}