package com.ranjan.droidbridge.di

import com.ranjan.droidbridge.domain.model.AppInfo

/** Set by the packaged app (see packaging/build-installers.sh) from the release tag, e.g. v1.2.0. */
const val VERSION_PROPERTY = "droidbridge.version"

/** Shown when running from source (`./kotlin run`), where no release version exists. */
const val DEV_VERSION = "0.0.0-dev"

/**
 * Build information shown in Help > About the Developer and used by Check for Updates.
 * Fill in the developer details and the GitHub repository that publishes releases.
 */
val DroidBridgeAppInfo = AppInfo(
    name = "DroidBridge",
    version = System.getProperty(VERSION_PROPERTY) ?: DEV_VERSION,
    developerName = "Ranjan",
    developerRole = "Android and macOS developer",
    developerEmail = null,
    developerWebsite = null,
    developerGithub = "RANZN",
    updateRepository = "RANZN/DroidBridge",
)
