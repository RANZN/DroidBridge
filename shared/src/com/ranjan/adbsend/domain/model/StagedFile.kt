package com.ranjan.adbsend.domain.model

/** A device file pulled into a temporary local folder so it can be handed to Finder. */
data class StagedFile(val transferId: Long, val localPath: String)
