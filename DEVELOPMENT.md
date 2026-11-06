# DroidBridge - development guide

DroidBridge is a macOS desktop app built with Kotlin, Compose Multiplatform (JVM) and the [Kotlin Toolchain](https://kotlin-toolchain.org/dev/) (Amper). It copies files over USB debugging (`adb push` / `adb pull`) or MTP (libmtp via JNA).

For what the app does and how to use it, see the [README](README.md).

## Build, run and test

```bash
./kotlin run -m desktopApp
```

```bash
./kotlin build
```

```bash
./kotlin test
```

Opt-in tests against real devices and services:

```bash
ADB_INTEGRATION=1 ADB_SERIAL=emulator-5554 ./kotlin test
```

```bash
GITHUB_UPDATE_REPO=RANZN/DroidBridge ./kotlin test
```

## Architecture

Clean Architecture with MVI, Koin for dependency injection.

```
shared/src/com/ranjan/droidbridge/            (common Kotlin)
  domain/model         Device, Transport, RemoteFile, Transfer, AppInfo, ...
  domain/repository    Interfaces (Device, MtpDevice, File, Transfer, Settings, Update)
  domain/usecase       One class per action, plus MtpFallbackPolicy
  presentation/mvi     MviViewModel<State, Intent, Effect>
  presentation/main    File browser: MainContract + MainViewModel + SelectionReducer
  presentation/help    Help menu: HelpContract + HelpViewModel

shared/src@jvm/com/ranjan/droidbridge/        (JVM / macOS specific)
  data/adb             AdbLocator, AdbClient (ProcessBuilder + pty for progress), AdbParsers
  data/mtp             libmtp JNA binding, MtpSession (single thread), MTP repositories
  data/transfer        TransferEngine per connection type (AdbTransferEngine, MtpTransferEngine)
  data/repository      Transfer queue, file routing (ADB/MTP), adb repositories, settings
  data/update          GitHub Releases update check
  di                   Koin modules, AppInfoConfig (developer details, update repository)
  ui                   Compose screens, menu bar, dialogs, drag and drop, app icon

desktopApp/src/main.kt                    Starts Koin and opens the window
desktopApp/icons/                         App icon as SVG, PNG (1024 px) and .icns
```

Rules of thumb:

- The UI only renders state and sends intents; one-off actions (snackbars, clipboard, opening links) arrive as effects.
- Domain code is plain Kotlin with no JVM or adb/libmtp details.
- Platform objects (`Preferences`, `HttpClient`, coroutine scopes) are created in Koin modules and injected.
- libmtp is not thread-safe: every call goes through `MtpSession`'s single thread.

## Configuration

`shared/src@jvm/com/ranjan/droidbridge/di/AppInfoConfig.kt` holds the version, the developer details shown in Help > About the Developer, and `updateRepository` (GitHub `owner/repo`) used by Check for Updates. Releases should be tagged like `v1.0.1`.

## Tool locations

- adb: `ADB_PATH`, `ANDROID_HOME`/`ANDROID_SDK_ROOT`, `~/Library/Android/sdk/platform-tools`, Homebrew paths, then `PATH`
- libmtp: `LIBMTP_PATH`, `/opt/homebrew/lib`, `/usr/local/lib`, `/opt/local/lib`

## Known limitations

- Drag out to Finder: Finder needs a real local file, so the item is pulled to a temp folder when Finder accepts the drop. For very large files the drop waits until that pull finishes.
- MTP on macOS: Image Capture, Photos, Android File Transfer and the `ptpcamerad` service can hold the phone. DroidBridge shows it as Busy and retries.
- MTP has no Wi-Fi or emulator support, and only shows shared storage.
- MTP has been unit-tested (bindings, paths, routing) but needs testing on more real phones.
- No packaged `.app` / `.dmg` yet; the `.icns` in `desktopApp/icons/` is ready for when packaging is set up.
