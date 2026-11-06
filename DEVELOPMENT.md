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

## Making changes

`main` is protected: changes go in through pull requests, and the **CI / Build and test** check must pass before merging. Direct pushes and force pushes to `main` are blocked.

```bash
git switch -c my-change
```

```bash
git push -u origin my-change && gh pr create --fill
```

## Releasing

1. Merge the changes into `main`.
2. Tag the release and push the tag:

   ```bash
   git tag v1.2.0 && git push origin v1.2.0
   ```

3. The **Release** workflow builds the installers on Apple Silicon and Intel runners and publishes a GitHub Release with:
   - `DroidBridge-<version>-macos-<arch>.pkg` - installer (double-click, Install)
   - `DroidBridge-<version>-macos-<arch>.dmg` - disk image (drag to Applications)
   - a `.sha256` checksum for each file

Tags must look like `v1.2.0` (macOS needs the first number to be 1 or higher). Tags with a suffix such as `v1.3.0-beta.1` are published as pre-releases. The version comes from the tag and is shown in Help > About; builds run from source show `0.0.0-dev`.

To test packaging without publishing, run **Actions > Release > Run workflow**; the installers are attached to the run as artifacts.

### Building installers locally

```bash
JAVA_HOME=/path/to/jdk-25 packaging/build-installers.sh v1.2.0
```

The script builds for the current Mac's architecture and writes to `build/dist/`. It needs a JDK 17+ that includes `jpackage` and `python3`.

Why not `./kotlin package -f executable-jar`? Two dependencies share the file name `runtime-desktop-1.12.1.jar`, and the executable JAR keeps only one, which breaks Compose at startup. `packaging/runtime_classpath.py` rebuilds the exact classpath from the resolved dependency graph instead.

### Code signing (not set up yet)

The app is only ad-hoc signed, so macOS asks users to confirm the first launch. Removing that prompt needs an Apple Developer account: a "Developer ID Application" certificate for `jpackage --mac-sign` and notarisation with `xcrun notarytool`, with the certificate and credentials stored as GitHub Actions secrets.

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
- Installers are not signed with a Developer ID or notarised yet (see Code signing above).
