package com.ranjan.droidbridge.domain.model

/** Static facts about this build and its developer, shown in About and used for update checks. */
data class AppInfo(
    val name: String,
    val version: String,
    val developerName: String,
    val developerRole: String? = null,
    val developerEmail: String? = null,
    val developerWebsite: String? = null,
    /** GitHub username, linked from About as https://github.com/<username>. */
    val developerGithub: String? = null,
    /** GitHub "owner/repo" whose releases are checked for updates; null disables update checks. */
    val updateRepository: String? = null,
)

/** The newest published release. */
data class ReleaseInfo(val version: String, val url: String, val notes: String?)

sealed interface UpdateCheck {
    data class UpToDate(val currentVersion: String) : UpdateCheck
    data class Available(val release: ReleaseInfo, val currentVersion: String) : UpdateCheck
}
