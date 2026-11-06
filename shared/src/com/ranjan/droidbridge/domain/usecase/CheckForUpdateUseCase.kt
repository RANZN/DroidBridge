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
