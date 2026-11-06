package com.ranjan.droidbridge.domain.usecase

import com.ranjan.droidbridge.domain.repository.TransferRepository

class PullFilesUseCase(private val repository: TransferRepository) {
    operator fun invoke(serial: String, remotePaths: List<String>, localDirectory: String) {
        if (remotePaths.isNotEmpty()) repository.pull(serial, remotePaths, localDirectory)
    }
}
