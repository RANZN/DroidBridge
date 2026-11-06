package com.ranjan.droidbridge.domain.usecase

/** Compares versions like "1.2.10" and "v1.3" numerically. */
object Versions {
    fun isNewer(candidate: String, current: String): Boolean {
        val a = parts(candidate)
        val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    private fun parts(version: String): List<Int> =
        version.trim().removePrefix("v").removePrefix("V")
            .substringBefore('-') // ignore "-beta" style suffixes
            .split('.')
            .map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
}
