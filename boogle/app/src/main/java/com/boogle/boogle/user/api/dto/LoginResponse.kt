package com.boogle.boogle.user.api.dto

data class LoginResponse (
    val accessToken: String,
    val adminName: String
)