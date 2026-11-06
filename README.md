# DroidBridge

macOS desktop app (Compose Multiplatform, JVM) for copying files between a Mac and Android devices using drag and drop. It connects over USB debugging (`adb push` / `adb pull`) or over the phone's normal File transfer mode (MTP, via libmtp).

## Features

- Lists every connected device (USB, emulator, Wi-Fi, MTP) with an ADB or MTP badge, and refreshes automatically
- Browse the device file system in list or grid view (ADB starts at `/sdcard`; MTP starts at the storage list)
- Drag files or folders from Finder onto the browser to copy them to the device
- Drag files or folders out to Finder to copy them to the Mac (only pulled when Finder accepts the drop)
- Right-click menus, Finder-style keyboard selection, rename, new folder, delete, download to a chosen folder
- Transfer queue with percentage progress: one transfer at a time per device, different devices in parallel
- Setup guides for USB debugging (ADB) and File transfer (MTP), with Skip and "Don't show again"
- Help menu: DroidBridge Help, Check for Updates (GitHub Releases), About the Developer

## ADB or MTP?

| | ADB (USB debugging) | MTP (File transfer) |
|---|---|---|
| Phone setup | Developer options + USB debugging | Choose "File transfer" in the USB notification |
| Mac setup | Android platform-tools (`adb`) | `brew install libmtp` |
| What you can see | Whole file system | Shared storage only (photos, downloads, documents) |
| Speed | Fastest | Slower |
| Wi-Fi / emulators | Yes | No, USB only |

MTP is off by default. Turn it on with the "MTP devices" switch at the bottom of the device list.

## Architecture

Clean Architecture with MVI, Koin for DI.

```
shared/src/com/ranjan/droidbridge/            (common Kotlin)
  domain/model         Device, Transport, RemoteFile, Transfer, AppInfo, ...
  domain/repository    Interfaces (Device, MtpDevice, File, Transfer, Settings, Update)
  domain/usecase       One class per action
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
  ui                   Compose screens, menu bar, dialogs, drag and drop

desktopApp/src/main.kt                    Starts Koin and opens the window
```

## Configuration

Edit `shared/src@jvm/com/ranjan/droidbridge/di/AppInfoConfig.kt` to set the version, developer details shown in About, and `updateRepository` (GitHub `owner/repo`) used by Check for Updates.

## Tool locations

- adb: `ADB_PATH`, `ANDROID_HOME`/`ANDROID_SDK_ROOT`, `~/Library/Android/sdk/platform-tools`, Homebrew paths, then `PATH`
- libmtp: `LIBMTP_PATH`, `/opt/homebrew/lib`, `/usr/local/lib`, `/opt/local/lib`

## Build and run

```bash
./kotlin run -m desktopApp
```

```bash
./kotlin test
```

Opt-in tests against real devices and services:

```bash
ADB_INTEGRATION=1 ADB_SERIAL=emulator-5554 ./kotlin test
```

```bash
GITHUB_UPDATE_REPO=owner/repo ./kotlin test
```

## Known limitations

- Drag out to Finder: Finder needs a real local file, so the item is pulled to a temp folder when Finder accepts the drop. For very large files the drop waits until that pull finishes.
- MTP on macOS: apps such as Image Capture, Photos, Android File Transfer and the `ptpcamerad` service can hold the phone. DroidBridge shows the phone as Busy and retries; quit those apps or run `killall ptpcamerad`.
- MTP has no Wi-Fi or emulator support, and only shows shared storage.
