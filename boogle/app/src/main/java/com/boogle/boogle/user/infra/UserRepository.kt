package com.boogle.boogle.user.infra

import com.boogle.boogle.user.domain.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {

    // LoginId로 User 조회 메서드
    fun findByLoginId(loginId: String): User?

}