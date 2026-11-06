package com.ranjan.droidbridge.data.adb

class AdbNotFoundException : IllegalStateException(
    "adb was not found. Install Android platform-tools (e.g. `brew install android-platform-tools`)."
)
