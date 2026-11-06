@file:Suppress("FunctionName", "PropertyName", "LocalVariableName")

package com.ranjan.droidbridge.data.mtp

import com.sun.jna.Callback
import com.sun.jna.Pointer

/** int (*LIBMTP_progressfunc_t)(uint64_t sent, uint64_t total, void const *data); non-zero cancels. */
fun interface MtpProgressCallback : Callback {
    fun invoke(sent: Long, total: Long, data: Pointer?): Int
}
