# WinUI for Kotlin & Java (winui4k)

[![Build](https://github.com/nttr-tech/winui4k/actions/workflows/build.yml/badge.svg)](https://github.com/nttr-tech/winui4k/actions/workflows/build.yml)
[![License: Apache 2.0](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE.txt)
![Kotlin](https://img.shields.io/badge/kotlin-2.4.0-7F52FF.svg?logo=kotlin)
![Java](https://img.shields.io/badge/java-8%2B-orange.svg)

English | [日本語](README.ja.md)

**WinUI** is the UI framework that Microsoft promotes as the standard for the Windows 11 generation.
With **WinUI4K**, you can build native Windows apps using WinUI in pure Kotlin or Java — no bridge DLL, no C#, no Visual Studio.
WinUI4K calls the WinRT ABI (the binary-level calling convention) directly from Java FFI (Panama / JNA / JNR), so there is no need to bundle a native DLL to bridge the language and the runtime.

This library was prototyped by [NTT Resonant Technology](https://nttr-tech.co.jp/), partly with the goal of applying it to the PC client of "[Remote TestKit](https://appkitbox.com/)", a service that lets you rent real smartphones over the Internet.
It is released under the Apache License 2.0 and can be used freely for both commercial and non-commercial purposes.

The screenshot below may look like Microsoft's WinUI 3 Gallery, but it is the bundled [Gallery app](winui4k-sample-gallery) written entirely in Kotlin.

![winui4k Gallery](doc/images/home.png)

## Table of Contents

- [Background](#background)
- [Example](#example)
- [Features](#features)
- [Comparison with Conventional WinUI Development](#comparison-with-conventional-winui-development)
- [Suitable Use Cases and Limitations](#suitable-use-cases-and-limitations)
- [Quick Start](#quick-start)
- [Sample Apps](#sample-apps)
- [Modules](#modules)
- [Architecture](#architecture)
- [Automatic Setup of the Windows App SDK Runtime](#automatic-setup-of-the-windows-app-sdk-runtime)
- [System Properties](#system-properties)
- [Contributing](#contributing)
- [License](#license)
- [References](#references)

## Background

Microsoft is positioning WinUI at the center of future Windows app UIs, and some of the standard Windows apps have already migrated to WinUI.
However, the development languages officially supported by WinUI are C++ and C#, and until now there has been almost no practical way to use it from Java or Kotlin, which are widely used in business systems.

As a result, teams with Java or Kotlin assets had to either rewrite everything in C# or switch to a different approach such as Electron, which builds desktop apps with web technologies, in order to adopt a native Windows UI.
Compose for Desktop is another option, but since it is a non-native approach with its own rendering engine, it cannot provide the OS-standard look and accessibility as-is.
winui4k was created to fill this gap by calling WinUI directly through JVM FFI.

## Example

You can build WinUI apps with a feel similar to Java Swing.

```kotlin
WinUiUtilities.invokeLater {
    val frame = WFrame(title = "WinUI4K")
    val nameField = WTextField(placeholder = "Name")
    val greetButton = WButton("Greet")

    greetButton.addActionListener {
        greetButton.text = "Hello, ${nameField.text.ifBlank { "world" }}!"
    }

    frame.add(nameField)
    frame.add(greetButton)
    frame.isVisible = true
}
```

This short snippet opens a native Windows window, and pressing the button changes its label.

## Features

- **60+ controls**: From Button / TextBox to NavigationView, TeachingTip, AppNotification, and AppWindow — all wrapped as `W*` classes. You can try them all in the Gallery.
- **Runs on Java 8**: FFI backends are pluggable. Three implementations are bundled: Panama (Java 22+, default), JNA (Java 8+), and JNR (Java 8+).
- **Coroutines support**: `Dispatchers.WinUi` (winui4k-extension-coroutines) dispatches to the UI thread, and `delay` runs on a native timer (DispatcherQueueTimer).
- **WebView2 support**: `WWebView` lets you embed the Microsoft Edge-based browser control in your app.
- **Accessibility**: Because OS-standard controls are used as-is, assistive technologies such as screen readers work out of the box.
- **E2E tests on real windows**: Tests launch actual WinUI windows for verification, and CI runs them on JDK 8 / 9 / 22 / 25.
- **No bridge DLL**: Everything — object creation (`RoGetActivationFactory`), WinUI's string type HSTRING, method calls through function tables (vtables), upcalls that expose Kotlin objects as COM objects, and COM aggregation — is implemented purely with JVM FFI ([Architecture](#architecture)).
- **Zero guessed ABI constants**: The identifiers (IIDs) and vtable positions required for COM calls are all machine-extracted from Windows type-information files (winmd) and contain no hand-written guesses.

## Comparison with Conventional WinUI Development

| Aspect | Conventional WinUI development | winui4k |
|---|---|---|
| Language | C# / C++ plus XAML markup | Kotlin / Java (UI is also written in code) |
| IDE | Typically Visual Studio | Any editor + JDK |
| Additional runtime/SDK | .NET SDK, Windows App SDK runtime | Windows App SDK runtime only (auto-installed at startup if missing) |
| Custom bridge DLL | Sometimes required | Not required |
| Existing JVM assets | Must be rewritten in C# | Can be used as-is |

The table above compares against typical development approaches; it may not apply to every configuration.

## Suitable Use Cases and Limitations

winui4k is a good fit when you want to:

- Modernize the UI of a Windows business app built with Swing or JavaFX to the OS-native Fluent Design (the standard design language of Windows 11)
- Use a native Windows UI in an environment that must run on Java 8
- Avoid the larger distribution size and memory usage of approaches like Electron that bundle a browser engine
- Get the look and accessibility of OS-standard controls rather than custom-rendered ones

On the other hand, there are the following limitations:

- **Windows only.** WinUI itself is Windows-only; if you need cross-platform support including macOS or Linux, consider alternatives such as Compose Multiplatform.
- **COM references are released in sync with GC**, so the timing is non-deterministic. If your use case creates and destroys UI elements at high frequency while requiring strict control over native-side release timing, your design must take this into account ([Architecture](#architecture)).
- **Cyclic references across the language boundary are not collected automatically.** Event listeners that are no longer needed must be removed explicitly with the corresponding remove methods.

## Quick Start

All you need is **JDK 25** (x64) — no Visual Studio, C++ build tools, or .NET SDK.
Get it from [Eclipse Temurin](https://adoptium.net/) or similar and add it to your PATH.

```powershell
git clone https://github.com/nttr-tech/winui4k.git
cd winui4k
.\gradlew run
```

This launches the Gallery app.
Even if the WinUI execution foundation (the Windows App SDK runtime) is not installed, it is set up automatically at startup ([details](#automatic-setup-of-the-windows-app-sdk-runtime)).

JDK 25 is required only for building the repository; the library itself runs on Java 8 or later.
You can verify running on Java 8 + JNA with `.\gradlew :winui4k-sample-gallery:runJna`, and on Java 8 + JNR with `.\gradlew :winui4k-sample-gallery:runJnr`.

The supported environment is Windows 11 x64 (expected to work on Windows 10 1809 or later as well).

## Sample Apps

In addition to the Gallery, several samples closer to real-world apps are bundled.
They all exist to demonstrate that real applications can be built with WinUI4K.

| Sample | Description | Run command |
|---|---|---|
| [Gallery](winui4k-sample-gallery) | Demo app showing 60+ controls by category (in the style of WinUI 3 Gallery) | `.\gradlew run` |
| [Filer](winui4k-sample-filer) | Fluent Design file manager with tabs, details/icon view switching, breadcrumbs, sidebar, and filtering | `.\gradlew :winui4k-sample-filer:run` |
| [Notes](winui4k-sample-notes) | Simple notepad app | `.\gradlew :winui4k-sample-notes:run` |
| [Form with MigLayout](winui4k-sample-form-with-miglayout) | Input form using the MigLayout layout library | `.\gradlew :winui4k-sample-form-with-miglayout:run` |

## Modules

| Module | Description |
|---|---|
| `winui4k` | The core. Public API (`W*` classes) and the internal COM / WinRT / WinUI layers |
| `winui4k-ffi-panama` | Panama (`java.lang.foreign`) FFI backend. Java 22+ |
| `winui4k-ffi-jna` | JNA FFI backend. Java 8+ (x64 only) |
| `winui4k-ffi-jnr` | JNR (jffi) FFI backend. Java 8+ (x86 / x64 / arm64) |
| `winui4k-extension-coroutines` | `Dispatchers.WinUi` (a WinUI counterpart of kotlinx-coroutines-swing) |
| `winui4k-extension-miglayout` | Adapter for laying out `W*` controls with the MigLayout layout library |
| `winui4k-all` | Aggregate module that references all of the above (excluding samples) |
| `winui4k-sample-gallery` | Demo app showcasing all controls |
| `winui4k-sample-filer` | Fluent Design file manager sample |
| `winui4k-sample-notes` | Notepad app sample |
| `winui4k-sample-form-with-miglayout` | Input form sample using MigLayout |

## Architecture

winui4k directly manipulates **COM** (the binary-level convention defined by Windows for calling objects across languages) through Java FFI, and calls WinUI on top of **WinRT** (the modern API foundation of Windows), which is an evolution of COM.

A reference to a COM object is a pointer to a struct whose first member is a pointer to an array of function pointers (the **vtable**).
Following `pointer → vtable → vtable[slot]` reaches the method implementation — the same mechanism as C++ virtual function calls.
WinUI's Button and Window exist in the process in this form, and winui4k assembles these calls directly with FFI.

The layers depend in one direction only.

| Layer | Package | Role |
|---|---|---|
| Public API | `com.appkitbox.winui4k` | `W*` classes (`WFrame` / `WButton` / ...) |
| WinUI | `internal.winui` | `*Interop` ABI constants, Dispatcher |
| WinRT | `internal.winrt` | HSTRING, `KComObject` (upcall stubs that expose Kotlin implementations as COM objects), Activation |
| COM | `internal.com` | `ComPtr`, Guid, converting HRESULT (COM call result codes) into exceptions |
| FFI SPI | `internal.ffi.api` | Backend-independent FFI vocabulary. Implementations are discovered via ServiceLoader |

FFI backends are separated into their own modules and are discovered and selected at runtime via ServiceLoader.
The priority order is Panama (default on Java 22+) > JNA (Java 8+, x64 only) > JNR (Java 8+, x86 / x64 / arm64), and you can also specify one explicitly with `-Dwinui4k.ffi`.
The core module targets Java 8, and only the Panama module references `java.lang.foreign`.

### Bridging COM References and GC

COM manages lifetimes with reference counting (`AddRef` / `Release`), while the JVM determines lifetimes by reachability with a tracing GC.
winui4k bridges this mismatch with the same design as **CsWinRT**, Microsoft's official interop runtime for C#.
Each `W*` wrapper owns exactly one COM reference count, and `Release` is called when the GC detects that the wrapper has become unreachable (via `java.lang.ref.Cleaner` on Java 9+, or an equivalent home-grown mechanism based on `PhantomReference` on Java 8).

COM has a threading convention called **apartments**: WinUI objects are bound to the UI thread, and `Release` must also be called from that thread.
Therefore, GC-triggered releases are funneled into the UI thread's message loop for execution.

Note that cyclic references crossing the language boundary (native → Kotlin-implemented event handler → Kotlin object → COM reference back to native) cannot be collected automatically.
CsWinRT solves this through mutual reference-graph queries between the .NET GC and the WinUI runtime, but the JVM's GC has no equivalent extension point.

### Known Limitations

- A single UI thread is assumed, and the `W*` API is contractually usable only on that thread.
- Window and Shell wrappers (`WFrame`, `WAppWindow`, etc.) are excluded from automatic release and hold their references indefinitely.
- Error handling is limited to converting HRESULTs into exceptions.

IIDs (interface identifiers) and vtable slot numbers are not hand-written guesses — they are machine-extracted from Windows type-information files (winmd) with `tools/dump_winmd.py`.

## Automatic Setup of the Windows App SDK Runtime

WinUI4K automatically sets up the Windows App SDK runtime — the execution foundation of WinUI — at app startup.

### Bootstrap DLL

The bootstrap DLL required to initialize the Windows App SDK (`Microsoft.WindowsAppRuntime.Bootstrap.dll`) is embedded in the winui4k JAR (covering the x86 / x64 / arm64 architectures).
On the first call to `WinUiUtilities`, the DLL matching the running PC's architecture is automatically extracted to a temporary directory and deleted when the process exits.

### Runtime Installation

The Windows App SDK 2.2 runtime is required. If it is not installed, the following steps are taken in order:

1. **Automatic installer execution**: If an installer such as `WindowsAppRuntimeInstall-x64.exe` exists in the current directory (or the directory specified by `winui4k.installer.dir`), it is run silently with the `--quiet` option and the app then starts normally
2. **Installation dialog**: If no installer is found, Microsoft's dialog is shown, prompting the user to download the runtime

The installers can be downloaded with the following command:

```powershell
.\gradlew :winui4k:downloadInstallers
```

Three installers — x86 / x64 / arm64 — are downloaded to `winui4k/installer/` (about 104 MB each).
If you bundle the installer matching the target architecture when distributing your app, the runtime is installed automatically on the end user's machine.

To install manually, run `WindowsAppRuntimeInstall-x64.exe` from https://aka.ms/windowsappsdk.

## System Properties

| Property | Value | Description |
|---|---|---|
| `winui4k.ffi` | `panama` / `jna` / `jnr` | Explicitly selects the FFI backend. Defaults to the highest-priority one available |
| `winui4k.lifetime` | `cleaner` / `phantom` | Explicitly selects the COM reference cleanup mechanism. Defaults to automatic selection based on the Java version |
| `winui4k.gcThreshold` | Integer (reference count) | Requests `System.gc()` whenever the number of live native references exceeds the threshold. Disabled by default |
| `winui4k.bootstrap.dll` | Path | Explicitly specifies a bootstrap DLL to use instead of the one embedded in the JAR |
| `winui4k.installer.dir` | Directory | Search location for runtime installers. Both absolute and relative paths are supported (defaults to the current directory) |

## Contributing

Bug reports, feature requests, and pull requests are welcome via GitHub Issues.

## License

[Apache License 2.0](LICENSE.txt). Free to use for both commercial and non-commercial purposes.

## References

- [WinUI development documentation](https://learn.microsoft.com/en-us/windows/apps/winui/winui3/)
   - [Fluent Design System](https://fluent2.microsoft.design/)
   - [Design basics for Windows apps](https://learn.microsoft.com/en-us/windows/apps/design/)

- [WinUI repository](https://github.com/microsoft/microsoft-ui-xaml)
   - [WinUI Gallery repository](https://github.com/microsoft/WinUI-Gallery)
   - [Windows App SDK repository](https://github.com/microsoft/WindowsAppSDK)
   - [nuget](https://www.nuget.org/packages/Microsoft.WindowsAppSDK)
- Additional WinUI components
   - [TableView repository](https://github.com/w-ahmad/WinUI.TableView)
   - [Community Controls repository](https://github.com/CommunityToolkit/Windows)
