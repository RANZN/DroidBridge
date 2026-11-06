package com.ranjan.droidbridge.domain.usecase

import com.ranjan.droidbridge.domain.repository.TransferRepository

class PushFilesUseCase(private val repository: TransferRepository) {
    operator fun invoke(serial: String, localPaths: List<String>, remoteDirectory: String) {
        if (localPaths.isNotEmpty()) repository.push(serial, localPaths, remoteDirectory)
    }
}
