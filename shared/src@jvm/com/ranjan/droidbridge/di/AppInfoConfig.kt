package com.ranjan.droidbridge.di

import com.ranjan.droidbridge.domain.model.AppInfo

/**
 * Build information shown in Help > About the Developer and used by Check for Updates.
 * Fill in the developer details and the GitHub repository that publishes releases.
 */
val DroidBridgeAppInfo = AppInfo(
    name = "DroidBridge",
    version = "1.0.0",
    developerName = "Ranjan",
    developerRole = "Android and macOS developer",
    developerEmail = null,
    developerWebsite = null,
    updateRepository = null, // e.g. "your-github-user/DroidBridge"
)
