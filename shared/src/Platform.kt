package com.evangelist.adbsend

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
