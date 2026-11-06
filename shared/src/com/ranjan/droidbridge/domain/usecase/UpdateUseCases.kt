package com.ranjan.droidbridge.domain.usecase

import com.ranjan.droidbridge.domain.model.AppInfo
import com.ranjan.droidbridge.domain.model.UpdateCheck
import com.ranjan.droidbridge.domain.repository.UpdateRepository

class CheckForUpdateUseCase(
    private val repository: UpdateRepository,
    private val appInfo: AppInfo,
) {
    suspend operator fun invoke(): Result<UpdateCheck> {
        val repo = appInfo.updateRepository
            ?: return Result.failure(IllegalStateException("Update checks are not set up for this build yet."))
        return repository.latestRelease(repo).map { release ->
            if (Versions.isNewer(release.version, appInfo.version)) {
                UpdateCheck.Available(release, appInfo.version)
            } else {
                UpdateCheck.UpToDate(appInfo.version)
            }
        }
    }
}

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
