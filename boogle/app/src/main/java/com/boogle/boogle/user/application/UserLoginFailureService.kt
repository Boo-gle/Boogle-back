package com.boogle.boogle.user.application

import com.boogle.boogle.user.infra.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class UserLoginFailureService (
    private val userRepository: UserRepository
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun increaseFailCount(userId: Long): Int {
        val user = userRepository.findById(userId).orElseThrow(){
            IllegalArgumentException("존재하지 않는 사용자입니다.")
        }
        user.increaseFailCount()
        userRepository.saveAndFlush(user)
        return user.failCount
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun resetFailCount(userId: Long) {
        val user = userRepository.findById(userId).orElseThrow(){
            IllegalArgumentException("존재하지 않는 사용자입니다.")
        }
        user.resetFailCount()
        userRepository.saveAndFlush(user)
    }

}