package com.ranjan.droidbridge.data.transfer

/** Progress of one transfer: overall [percent] and the file currently being copied. */
data class TransferProgress(val percent: Int, val item: String?)
