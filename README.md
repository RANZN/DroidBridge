# ADBSend

macOS desktop app (Compose Multiplatform, JVM) for copying files between a Mac and Android devices using drag and drop. Internally it runs `adb push` / `adb pull`.

## Features

- Lists every connected device (USB, emulator, Wi-Fi) and refreshes automatically
- Browse the device file system (starts at `/sdcard`)
- Drag files or folders from Finder onto the browser -> `adb push` into the current folder
- Drag files or folders out to Finder -> `adb pull` (staged in a temp folder first, then copied by Finder)
- Download selected items to a chosen folder, create folders, delete items
- Transfer queue: one transfer at a time per device, different devices in parallel
- Setup guide for enabling USB debugging when no device is connected, with Skip and "Don't show again"

## Architecture

Clean Architecture with MVI, Koin for DI.

```
shared/src/com/ranjan/adbsend/            (common Kotlin)
  domain/model         Device, RemoteFile, Transfer, AdbStatus
  domain/repository    Interfaces (DeviceRepository, FileRepository, TransferRepository, SettingsRepository)
  domain/usecase       One class per action
  presentation/mvi     MviViewModel<State, Intent, Effect>
  presentation/main    MainContract (State/Intent/Effect) + MainViewModel

shared/src@jvm/com/ranjan/adbsend/        (JVM / macOS specific)
  data/adb             AdbLocator, AdbClient (ProcessBuilder), AdbParsers
  data/repository      adb-backed repository implementations, java.util.prefs settings
  di                   Koin modules
  ui                   Compose screens, drag and drop, Finder helpers

desktopApp/src/main.kt                    Starts Koin and opens the window
```

## adb location

The app looks for adb in this order: `ADB_PATH`, `ANDROID_HOME`/`ANDROID_SDK_ROOT`, `~/Library/Android/sdk/platform-tools`, Homebrew paths, then `PATH`.

## Build and run

```bash
./kotlin run -m desktopApp
```

```bash
./kotlin test
```

## Known limitations

- Drag out to Finder: Finder needs a real local file, so the item is pulled to a temp folder when the drag starts. For very large files the drop waits until that pull finishes.
- adb does not report progress when not attached to a terminal, so transfers show as running/completed rather than a percentage.
