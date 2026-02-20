package com.boogle.boogle.config

import com.boogle.boogle.global.jwt.JwtAuthenticationFilter
import com.boogle.boogle.global.jwt.JwtProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfig (
    private val jwtProvider: JwtProvider
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    // 개발용
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { it.disable() }
            .cors {}
            .authorizeHttpRequests { auth -> auth.anyRequest().permitAll() }
        return http.build()
    }

    // 배포용
//    @Bean
//    fun filterChain(http: HttpSecurity): SecurityFilterChain {
//        http.csrf { it.disable() }
//            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
//            .authorizeHttpRequests { auth ->
//                auth
//                    .requestMatchers("/admin/login").permitAll()
//                    .requestMatchers("/admin/**").hasRole("ADMIN")
//                    .anyRequest().permitAll()
//            }
//            .addFilterBefore(
//                JwtAuthenticationFilter(jwtProvider),
//                UsernamePasswordAuthenticationFilter::class.java
//            )
//        return http.build()
//    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()
        config.addAllowedOrigin("http://localhost:3000")
        config.addAllowedMethod("*")
        config.addAllowedHeader("*")
        config.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }

}