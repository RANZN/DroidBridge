package com.ranjan.droidbridge.domain.repository

import com.ranjan.droidbridge.domain.model.ReleaseInfo

interface UpdateRepository {
    suspend fun latestRelease(repository: String): Result<ReleaseInfo>
}
