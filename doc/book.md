# Part I: Overview

## Chapter 1: What Is [WinUI4K](https://github.com/nttr-tech/winui4k)?

WinUI is the UI framework Microsoft promotes as the standard for the Windows 11 generation.
It is distributed separately from the OS as part of the Windows App SDK and runs on Windows 10 version 1809 or later.

The languages WinUI officially targets are C++ and C#; until now there has been no practical way to use it from Java or Kotlin.
[WinUI4K](https://github.com/nttr-tech/winui4k) is a Kotlin library that fills this gap by calling WinUI directly through the JVM's FFI.
You can write WinUI apps in nothing but Kotlin or Java — no bridge DLLs, no C#, no Visual Studio.

This chapter explains the problem [WinUI4K](https://github.com/nttr-tech/winui4k) solves, its design philosophy, how it compares to similar technologies, and the points to weigh when deciding whether to adopt it.

### 1.1 The Problem It Solves

Until now, there has been effectively no way to write native Windows UI from the JVM.

- Swing's Windows look-and-feel merely imitates the OS theme visually. The controls themselves live on the Java side; they are not real OS controls.
- SWT wraps OS widgets directly, but its targets are Win32-generation controls; it does not support WinUI.
- JavaFX and Compose for Desktop use their own rendering engines to draw everything themselves — they resemble the OS look but are not the real thing.

Imitation and self-rendering approaches have the following problems.

- **Keeping up with the look**: Fluent Design, the standard design language of Windows 11, evolves with OS updates, so imitation approaches face a never-ending chase.
- **Accessibility**: Assistive technologies such as screen readers read the OS's UI Automation information. For a self-rendering approach to achieve equivalent support, the toolkit must reimplement the connection to assistive technologies, and the quality of support depends on how complete that reimplementation is. Assistive-technology support can be a procurement requirement in public-sector and enterprise projects.
- **Fine details of input and display**: Differences from the OS standard show up in IME handling, touch, DPI scaling, and so on.

For these reasons, when a shop with JVM assets needed native Windows UI, the only options were a full rewrite in C# or a move to a browser-engine-bundling approach like Electron.
The former means abandoning existing code and skill sets; the latter means giving up on native UI and accepting larger distribution sizes and memory usage.

[WinUI4K](https://github.com/nttr-tech/winui4k) solves this problem by calling WinUI directly through the JVM's FFI (a mechanism for calling functions in other languages directly from Java).
This is possible because, within a process, WinUI objects exist as COM objects (COM being the binary-level convention Windows defines for calling objects across languages).
COM is a binary convention independent of any particular language, so if you assemble calls that follow the convention via FFI, there is no need to go through C# or C++.
Object creation, method calls, event handler registration, even synthesizing a subclass of `Application` — every required operation is implemented purely with the JVM's FFI (details in Part V).

As a result, you can use more than 60 WinUI controls from nothing but Kotlin and Java code.
The runtime requirement is Java 8 or later, and apps can be distributed as ordinary JARs.

### 1.2 Design Philosophy

The design of [WinUI4K](https://github.com/nttr-tech/winui4k) rests on the following four principles.

**No bridge DLL.**
Rather than building a custom DLL to translate between the JVM and native code, it assembles the COM calling convention directly via FFI.
This means development requires no Visual Studio or C++ build toolchain, and there is no maintenance burden of building, signing, and bundling DLLs for each of the x86 / x64 / ARM64 architectures.
Note that the official Microsoft bootstrap DLL required to initialize the Windows App SDK is embedded in the JAR, but this is an initialization DLL distributed by Microsoft, not a custom-built bridge.

**Provide a Swing-style imperative API.**
No XAML is written; screens are assembled in code.

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

The vocabulary — `invokeLater`, `addActionListener`, `frame.add` — follows Swing.
Because the primary audience is shops with existing Swing or JavaFX assets, code can be ported through correspondences like `JFrame` → `WFrame` and `JButton` → `WButton`, without assuming the learning of a new UI paradigm.

**Use OS-standard controls as they are.**
The `W*` classes are thin wrappers over WinUI controls and contain no reimplementation of rendering.
The look, accessibility, IME handling, and DPI scaling come straight from the quality of the OS implementation, and when Fluent Design is updated on the OS side, the app's look follows along without a rebuild.

**Never rely on guesswork.**
The identifiers (IIDs) and function-table positions (vtable slots) needed for COM calls are all values mechanically extracted from winmd, Windows's type-information files; there are no hand-written guessed values.
Tests are E2E, actually launching WinUI windows, and CI runs them on JDK 8 / 9 / 22 / 25.

One further design choice is lowering the runtime requirement all the way to Java 8.
The FFI backend is pluggable: Java 22 and later use the standard Panama API, while older environments use JNA or JNR (Chapter 7).

### 1.3 Comparison with Similar Technologies

| Technology | What the UI actually is | Development language | Supported OS | Distribution characteristics |
|---|---|---|---|---|
| Conventional WinUI development | Native (WinUI) | C# / C++ with XAML | Windows | .NET runtime + Windows App SDK runtime |
| [WinUI4K](https://github.com/nttr-tech/winui4k) | Native (WinUI) | Kotlin / Java | Windows | JVM + Windows App SDK runtime |
| SWT | Native (Win32 / GTK, etc.) | JVM languages | Windows / macOS / Linux | JVM + per-OS native libraries |
| Swing | Self-rendered (imitating OS themes) | JVM languages | Windows / macOS / Linux | JVM only |
| JavaFX | Self-rendered | JVM languages | Windows / macOS / Linux | JVM + JavaFX runtime |
| Compose for Desktop | Self-rendered (Skia) | Kotlin | Windows / macOS / Linux | JVM + Skia |
| Electron | Browser-rendered (Chromium) | JavaScript / TypeScript | Windows / macOS / Linux | Bundles Chromium + Node.js |

Guidance for choosing between them is as follows.

- **Conventional WinUI development (C#)**: If your team has a .NET skill set, writing in C# is the orthodox approach. [WinUI4K](https://github.com/nttr-tech/winui4k) targets shops with JVM assets and skill sets.
- **Compose for Desktop / JavaFX**: If cross-platform support is a must, the Windows-only [WinUI4K](https://github.com/nttr-tech/winui4k) drops out of consideration, and these self-rendering approaches are the sensible choice. Self-rendering's weakness ("not the real OS controls") and strength ("a unified look across multiple OSes") are two sides of the same coin, and the evaluation shifts with the requirements.
- **SWT**: The design philosophy of wrapping OS widgets to get a native look is the same as [WinUI4K](https://github.com/nttr-tech/winui4k)'s; [WinUI4K](https://github.com/nttr-tech/winui4k) amounts to the WinUI version of it. SWT wraps Win32-generation controls with per-OS native libraries, while [WinUI4K](https://github.com/nttr-tech/winui4k) wraps the WinUI generation via FFI with no custom native code. If Fluent Design is a requirement, SWT cannot get you there.
- **Electron**: A rational choice for shops with web skill sets and assets. If you want to avoid the distribution size and memory usage of bundling a browser engine, or if the look of OS-standard controls and assistive-technology support are requirements, [WinUI4K](https://github.com/nttr-tech/winui4k) has the advantage.

In summary, [WinUI4K](https://github.com/nttr-tech/winui4k) is a fit when three conditions line up: "you have JVM assets," "Windows-only is acceptable," and "you need the native look and accessibility."

### 1.4 Points for Adoption Decisions

**Runtime requirements.**
The execution environment is Windows 11 (expected to work on Windows 10 version 1809 or later as well) and Java 8 or later.
The Windows App SDK runtime, WinUI's execution foundation, is required, but on machines without it, it is set up automatically when the app starts (Chapter 2).
Building the repository requires JDK 25, but that is a development-side requirement; the library's runtime requirement remains Java 8.

**Performance characteristics.**
Because no browser engine is bundled, the baseline distribution size and memory usage are smaller than the Electron approach.
Rendering and layout computation are handled by the OS-side WinUI runtime; the JVM side's work is limited to application logic and FFI calls.
On the other hand, JVM process startup and Windows App SDK initialization add to startup time.

**License.**
Apache License 2.0 — usable in both commercial and non-commercial settings.
The Windows App SDK runtime it depends on is distributed by Microsoft, which permits bundling and redistributing the installer.

**Maintenance status.**
[WinUI4K](https://github.com/nttr-tech/winui4k) is a library developed by NTT Resonant Technology, with one of its goals being use in the PC client of its own service, Remote TestKit.
Deliberate design trade-offs and known limitations are documented explicitly, and bug reports and feature requests are accepted via GitHub Issues.

**Constraints that affect your design.**

- **Windows only**: A constraint of WinUI itself; it cannot be overturned. The moment cross-platform requirements arise, a rewrite is needed.
- **COM reference release is tied to GC**: Native-side references are released when the `W*` wrapper is collected by GC, so the release timing is nondeterministic. In workloads that create and destroy UI elements at high frequency and need strict control over when native resources are freed, you must design with this premise in mind (Chapter 6).
- **Cyclic references across the language boundary are not collected automatically**: A cycle of native → Kotlin event handler → native is invisible to the JVM's GC. The operating assumption is that event listeners no longer needed are removed explicitly via remove-style methods (Chapters 6 and 8).
- **There is a single UI thread**: The contract is that the `W*` API is used only on the UI thread (Chapter 5).
- **Error handling is HRESULT-to-exception only**: COM call result codes are converted to exceptions and nothing more; there is no richer typed error hierarchy (Chapter 14).
- **The wrapped API surface is a subset of WinUI**: More than 60 controls are enough to build practical apps, but not every WinUI API is wrapped. Unwrapped features can be reached by calling the internal layers directly (Chapter 12).

## Chapter 2: Quick Start

This chapter walks from adding the dependency to getting an app running where "pressing a button makes something happen."
It also collects the initial errors that commonly trip people up and the bundled samples useful for verification.

### 2.1 Environment Requirements and Adding the Dependency

The runtime environment has two requirements.

- **OS**: Windows 11 x64 (expected to work on Windows 10 version 1809 or later as well)
- **Java**: Java 8 or later. The JDK's architecture must match the OS (an x64 JDK for x64 Windows)

The Windows App SDK runtime, WinUI's execution foundation, is also required, but if it is not installed it is set up automatically the first time the app starts (Section 2.4), so no advance preparation is needed.
Visual Studio, C++ build tools, and the .NET SDK are not used.

The library is split into modules.
The setup is the core `winui4k` plus at least one FFI backend (the native-call implementation) on the classpath.

| Module | Contents | Supported environment |
|---|---|---|
| `winui4k` | Core (required) | Java 8 or later |
| `winui4k-ffi-panama` | Panama (`java.lang.foreign`) backend. Default | Java 22 or later |
| `winui4k-ffi-jna` | JNA backend | Java 8 or later (x64 only) |
| `winui4k-ffi-jnr` | JNR backend | Java 8 or later (x86 / x64 / ARM64) |
| `winui4k-extension-coroutines` | `Dispatchers.WinUi` (optional) | - |
| `winui4k-extension-miglayout` | MigLayout adapter (optional) | - |
| `winui4k-all` | Aggregate reference to all of the above | - |

When in doubt, the easy option is the all-in-one `winui4k-all`.

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.appkitbox.winui4k:winui4k-all:0.1.0") // substitute the latest version
}
```

If you want to trim the distribution size, pick just the core and the backends you need.

```kotlin
dependencies {
    implementation("com.appkitbox.winui4k:winui4k:0.1.0")
    implementation("com.appkitbox.winui4k:winui4k-ffi-panama:0.1.0") // for Java 22 or later
    // implementation("com.appkitbox.winui4k:winui4k-ffi-jna:0.1.0") // for Java 8-21
}
```

When multiple backends are present, one is selected automatically by priority (Panama > JNA > JNR), so you normally do not need to think about the choice.
How to switch explicitly is covered in Chapter 7.

### 2.2 Minimal Code to Show a Single Window

The minimal app is as follows.

```kotlin
import com.appkitbox.winui4k.WFrame
import com.appkitbox.winui4k.WinUiUtilities

fun main() {
    WinUiUtilities.invokeLater {
        val frame = WFrame(title = "Hello WinUI4K")
        frame.isVisible = true
    }
}
```

Run it and a single native Fluent Design window opens.
Closing the window also exits the app.

`WinUiUtilities.invokeLater` corresponds to Swing's `SwingUtilities.invokeLater` and runs the given block on the UI thread.
If WinUI has not started yet, the first call also performs the startup work automatically (extracting the bootstrap DLL, initializing the Windows App SDK, starting the message loop).
All operations on `W*` classes are contractually performed on the UI thread, so UI construction always goes inside the `invokeLater` block (the threading model is detailed in Chapter 5).

Note that when running a jar directly with the Panama backend, the native-access permission option is required.

```powershell
java --enable-native-access=ALL-UNNAMED -jar app.jar
```

### 2.3 A Minimal App with a Button and an Event Handler

Next, place a text input and a button and respond to clicks.

```kotlin
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WFrame
import com.appkitbox.winui4k.WTextField
import com.appkitbox.winui4k.WinUiUtilities

fun main() {
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
}
```

The building blocks follow the same thinking as Swing.

- Controls are created with constructors and added with `frame.add`. They stack vertically in the order added (layout control is in Chapter 9).
- Events are received via listener registration such as `addActionListener`. Listeners are invoked on the UI thread, so they can update the UI directly.
- Long-running work inside a listener freezes the UI. Offload heavy work to a worker thread and bring only the result back to the UI thread via `invokeLater` (Chapters 5 and 10).

### 2.4 Automatic Setup of the Windows App SDK Runtime

Running a WinUI app requires the Windows App SDK 2.2 runtime.
So that neither developers nor end users have to think about this, [WinUI4K](https://github.com/nttr-tech/winui4k) performs a two-stage automatic setup at startup.

**Extracting the bootstrap DLL.**
The bootstrap DLL required to initialize the Windows App SDK (`Microsoft.WindowsAppRuntime.Bootstrap.dll`) is embedded in the winui4k JAR for all three architectures: x86 / x64 / ARM64.
On the first API call, the DLL matching the running PC's architecture is automatically extracted to a temporary directory and deleted when the process exits.
The app does not need to provide or place any DLL itself.

**Installing the runtime.**
If the runtime itself is not installed, the following steps are taken in order.

1. If an installer such as `WindowsAppRuntimeInstall-x64.exe` is present in the current directory (or the directory specified by `winui4k.installer.dir`), a silent install runs and the app starts as usual.
2. If no installer is found, a Microsoft dialog appears prompting the user to download the runtime.

When distributing to end users, bundling the installer matching the target architecture lets setup complete without any user interaction.
The installers can be fetched with the following command (three variants — x86 / x64 / ARM64, about 104 MB each — are downloaded into `winui4k/installer/`).

```powershell
.\gradlew :winui4k:downloadInstallers
```

To install manually, run `WindowsAppRuntimeInstall-x64.exe` from https://aka.ms/windowsappsdk.
Detailed configurations for each distribution form are covered in Chapter 15.

### 2.5 Initial Troubles

Here is a summary of the points that commonly trip people up on first launch.
In every case the symptom is printed to the console with an HRESULT (a COM error code), so check the console output first.

- **`MddBootstrapInitialize2 failed` (e.g., HRESULT=0x80670016)**: The Windows App SDK 2.2 runtime is not installed, or the major version does not match. Install a 2.2-series runtime from https://aka.ms/windowsappsdk.
- **`REGDB_E_CLASSNOTREG` (0x80040154) appears**: WinUI type resolution was attempted while the bootstrap had not succeeded. As above, check whether the runtime is installed.
- **The window appears but controls are not shown**: Applying the controls' default styles (`XamlControlsResources`) failed. Check the HRESULT on the console.
- **A warning or error about `--enable-native-access` appears**: When using the Panama backend and running a jar directly, `java --enable-native-access=ALL-UNNAMED` must be supplied (Section 2.2). Running via Gradle's `run` task adds it automatically.
- **Does not start on Java 8-21**: The Panama backend is exclusive to Java 22 and later. Add JNA (x64) or JNR to the classpath.
- **Does not start on ARM64 Windows**: The JDK itself must be an ARM64 build. Also, the JNA backend is x64-only, so use Panama or JNR on ARM64.

As a general note on execution environments, WinUI — including the E2E tests — requires an actual desktop session.
It does not work in headless CI environments or session-less service processes (CI handling is covered in Chapter 13).

### 2.6 A Tour of the Bundled Sample Apps

The repository bundles sample apps that serve both as smoke tests and as implementation examples.
Clone the repository and they can be launched with nothing but JDK 25.

```powershell
git clone https://github.com/nttr-tech/winui4k.git
cd winui4k
.\gradlew run
```

| Sample | Contents | Launch command |
|---|---|---|
| Gallery | A demo listing 60+ controls by category (in the style of WinUI 3 Gallery) | `.\gradlew run` |
| Filer | A Fluent Design file manager. Tabs, view switching, breadcrumbs, sidebar, filters | `.\gradlew :winui4k-sample-filer:run` |
| Notes | A simple notepad app | `.\gradlew :winui4k-sample-notes:run` |
| Form with MigLayout | An input form using MigLayout | `.\gradlew :winui4k-sample-form-with-miglayout:run` |

Each sample's role is as follows.

- **Gallery** is the catalog for checking "what controls exist" and "how they look." This book has no component catalog, so use Gallery to browse for controls. Once you find a control you want to use, its source code serves as the implementation example.
- **Filer and Notes** are practically oriented examples combining multiple controls. Read them as real examples of screen composition, event handling, and theme support.
- **Form with MigLayout** is an example of combining with the MigLayout layout library (Chapter 9).

For verifying behavior on Java 8, there are dedicated tasks that launch Gallery with JDK 8 + JNA / JNR.

```powershell
.\gradlew :winui4k-sample-gallery:runJna   # JDK 8 + JNA
.\gradlew :winui4k-sample-gallery:runJnr   # JDK 8 + JNR
```

Gradle's foojay resolver fetches the required JDKs automatically, so there is no need to prepare JDK 8 by hand.

# Part II: Core Concepts

## Chapter 3: Architecture Overview

This chapter lays out the overall structure of [WinUI4K](https://github.com/nttr-tech/winui4k).
It defines the layer structure and dependency directions, the intent behind the module split, and the terminology used throughout this book.
All subsequent chapters assume the vocabulary established here.

### 3.1 Layer Structure and Dependency Direction

The core module of [WinUI4K](https://github.com/nttr-tech/winui4k) (`winui4k`) is organized on the principle of one technology-stack layer = one package.
Dependencies flow one way, top to bottom, never the reverse.

| Layer | Package | Role |
|---|---|---|
| Public API | `com.appkitbox.winui4k` | `WinUiUtilities` and the `W*` classes (`WFrame` / `WButton` / ...) |
| WinUI | `internal.winui` | `*Interop` objects of ABI constants, `Dispatcher`, the Windows App SDK `Bootstrap` |
| WinRT | `internal.winrt` | `Hstring`, `KComObject` (exposing Kotlin implementations as COM objects), `Activation`, `Async`, `Pinterface` |
| COM | `internal.com` | `ComPtr` (vtable calls), `Guid`, `checkHr` (HRESULT-to-exception), `lifetime` (automatic reference release) |
| FFI SPI | `internal.ffi.api` | Backend-independent FFI vocabulary (`Ptr` / `CallDescriptor` / `FfiBackend`) |

This division has two intents.

- **Minimizing the public surface**: The public API is the root package only; everything else lives under `internal`. The area users must learn is confined to the `W*` classes, leaving room to rework the internals without promising compatibility.
- **Localizing knowledge**: General COM conventions (reference counting, QueryInterface) are confined to `internal.com`, WinRT-specific extensions (HSTRING, activation) to `internal.winrt`, and WinUI-specific values (IIDs, vtable slots) to `internal.winui`. For example, the only thing that knows "the slot number of Button's Click event" is `XamlInterop` in `internal.winui`.

The implementation of each layer is examined in Part V.

### 3.2 Module Split

The module lineup is as in the table in Section 2.1; the reasons for the split are the following three.

**To keep the core targeting Java 8.**
The `winui4k` core is compiled with `-Xjdk-release=8` and has no compile-time references to Java 9+ APIs.
Code that depends on the JDK version (Panama's `java.lang.foreign` requires Java 22 or later) cannot live in the core, so the FFI backends are split into separate modules and discovered at runtime via ServiceLoader.
Only `winui4k-ffi-panama` references `java.lang.foreign`, and only `winui4k-ffi-jna` references `com.sun.jna`.

**To make optional dependencies opt-in.**
Both `kotlinx-coroutines` and MigLayout bring in external library dependencies, so they are separated out as extension modules (`winui4k-extension-coroutines` / `-miglayout`).
Apps that do not use them can leave them out of their distribution.

**To provide an entry point that skips the choosing.**
`winui4k-all` is an aggregate module referencing all of the above.
Use it when you want the dependency done in one line; use individual selection when trimming distribution size.

### 3.3 Glossary

Here are the terms used repeatedly in this book.

| Term | Meaning |
|---|---|
| COM | The binary-level convention Windows defines for calling objects across languages |
| WinRT | An evolution of COM. Adds the common base IInspectable, the string type HSTRING, and winmd-format metadata. The reference-management conventions are the same as COM |
| Interface pointer | A reference to a COM object. A pointer to a structure whose first member is a pointer to a vtable |
| vtable | An array of function pointers. Following `pointer → vtable → vtable[slot number]` reaches the method implementation |
| IID | A GUID identifying an interface |
| QueryInterface (QI) | The operation of asking the same object for another of its interfaces by IID |
| HSTRING | WinRT's string type |
| HRESULT | COM's call result code. Negative values mean failure |
| Apartment | COM's threading convention. STA objects are bound to a specific thread; MTA objects can be called from any thread |
| downcall / upcall | Calling a native function from the JVM / native code calling back into JVM code |
| RCW / CCW | A wrapper for using a native COM object from the JVM (the `W*` classes) / a wrapper for exposing a Kotlin implementation to native code as a COM object (`KComObject`) |
| winmd | Windows's type-information files. The primary source for IIDs and method order in vtables |
| DIP | Device-independent pixel. WinUI's logical pixel. All layout coordinates in the `W*` API use this unit |

## Chapter 4: Application Lifecycle

The life of a [WinUI4K](https://github.com/nttr-tech/winui4k) app has three stages: startup → message loop → shutdown.
All that user code sees is the block passed to `invokeLater`, but knowing what happens behind it helps you understand the initialization-timing constraints (theme resources, window creation) and the shutdown cautions (the anti-patterns of Chapter 14).

### 4.1 Startup Sequence

The first call to `WinUiUtilities.invokeLater` is the startup trigger.
At that point a dedicated UI thread (thread name `WinUI4K-UI`, non-daemon) starts, and initialization proceeds in the following order.

1. **DPI declaration**: Declares Per-Monitor v2 DPI awareness for the process via `SetProcessDpiAwarenessContext`. Because java.exe carries no DPI-awareness manifest, the declaration must be made in code (Section 9.4).
2. **Windows App SDK bootstrap**: Extracts the JAR-embedded bootstrap DLL to a temporary directory and binds the Windows App SDK 2.2 runtime to the process via `MddBootstrapInitialize2`. On failure, it attempts to run the installer automatically (Section 2.4).
3. **Joining COM**: Calls `RoInitialize(RO_INIT_SINGLETHREADED)`. This thread becomes an STA, and from then on all XAML objects are bound to this thread (Chapter 5).
4. **`Application.Start`**: Called with a callback. This call blocks as the message loop and does not return until the app exits.
5. **Synthesizing the `Application` subclass**: Inside the callback, a "subclass" of `Application` is synthesized via COM aggregation. The equivalent of C#'s `class App : Application` is built by fusing a Kotlin-implemented COM object with the native-side base implementation (the mechanism is in Section 16.5). At the same time, the `ResourceManagerRequested` handler needed to resolve theme resources is registered (Section 11.3).
6. **`OnLaunched`**: The XAML runtime calls back into the synthesized subclass's `OnLaunched`. Here the UI thread's `DispatcherQueue` is captured, and the controls' default styles (`XamlControlsResources`) are added to `Application.Resources`. This is because of a WinUI-side constraint that `Application.Resources` must not be touched before this point.
7. **Running user code**: Finally, the block passed to `invokeLater` is executed.

For users there is just one takeaway.
All UI construction happens inside the `invokeLater` block — that is, from step 7 onward — so there is no need to think about initialization order.

### 4.2 The Message Loop and Event Dispatch

While `Application.Start` is blocking, the UI thread keeps spinning WinUI's message loop.
There are two routes by which work reaches this loop.

**Posting via invokeLater.**
`WinUiUtilities.invokeLater` puts the given block onto a FIFO queue and calls `TryEnqueue` on the UI thread's `DispatcherQueue`.
The handler passed to `TryEnqueue` is a Kotlin-implemented COM object (Section 16.4); when the message loop calls it back, it takes blocks off the queue and runs them.
It is safe to call from any thread, and execution order matches posting order.

**Event upcalls.**
Events raised on the WinUI side, such as a button click, arrive by the message loop calling back the delegate passed at listener registration (also a Kotlin-implemented COM object).
That is, listeners always run on the UI thread and can update the UI directly (Chapter 8).

For both routes, execution happens "after the currently processing message finishes."
If work on the UI thread drags on, all subsequent rendering, input, and events stall.
This is the reason behind the principle of offloading heavy work to worker threads (Chapters 5 and 10).

### 4.3 Shutdown Sequence

By default, the app exits when the last `WFrame` is closed (`WinUiUtilities.exitOnLastWindowClosed`, default true).
If you want the loop to continue after the last window closes (a resident app, for example), set it to false and call `WinUiUtilities.exit()` at a time of your choosing.
`exit()` can be called from any thread.

The internal shutdown proceeds in this order.

1. `Application.Exit` ends the message loop, and `Application.Start` returns.
2. `ReleasePump.shutdown()` stops the GC-driven release of COM references (Chapter 6). Release requests after this are discarded, and anything not yet released is left to process termination.
3. `RoUninitialize` closes the COM apartment, and `MddBootstrapShutdown` detaches the Windows App SDK.

The order of steps 2 and 3 matters.
Calling `Release` after `RoUninitialize` crashes, so the release path is closed first ("not chasing the unreleased remainder" is the same trade-off CsWinRT makes; Chapter 18).
Also, skipping `RoUninitialize` itself causes callbacks from native threads during JVM shutdown to fail to attach, aborting the whole JVM.
Do not bypass this path with `System.exit` or the like (Chapter 14).

Note that the message loop can only be started once per JVM process.
Calling `invokeLater` after shutdown throws `IllegalStateException`.
This constraint also affects test design (Chapter 13).

## Chapter 5: Threading Model

"UI freezes" and "crashes from touching the UI on another thread" are the two classic accidents of GUI development.
This chapter explains [WinUI4K](https://github.com/nttr-tech/winui4k)'s threading contract for avoiding them, and the tools for honoring the contract.

### 5.1 The Single-UI-Thread Contract and COM Apartments

The threading contract of [WinUI4K](https://github.com/nttr-tech/winui4k) can be stated in one sentence.
**The `W*` API is used only on the UI thread.**

This contract is not something [WinUI4K](https://github.com/nttr-tech/winui4k) imposed for convenience; it derives from COM's apartment conventions.
At startup, the UI thread is initialized as an STA via `RoInitialize(RO_INIT_SINGLETHREADED)` (Section 4.1), and all XAML objects are bound to this thread.
Calling a XAML object's methods from another thread violates COM's conventions — it will not necessarily end at an exception, and can cause crashes or undefined behavior.

The idea is the same as Swing's EDT or JavaFX's Application Thread, but consider the consequences of violations to be harsher (things break outside the JVM).
Whether the current thread is the UI thread can be checked with `WinUiUtilities.isDispatchThread`, useful as an assertion at the entry points of your own components.

Note that the UI thread is a dedicated thread that [WinUI4K](https://github.com/nttr-tech/winui4k) starts (`WinUI4K-UI`); it is not the `main` thread.
After calling `invokeLater`, `main` is free to do as it likes (the process will not exit as long as the non-daemon UI thread is alive).

### 5.2 invokeLater and the Dispatcher

There are two basic APIs for delivering work to the UI thread.

- **`WinUiUtilities.invokeLater(block)`**: Posts the block to the UI thread's message loop. Callable from any thread; execution order matches posting order. Corresponds to Swing's `SwingUtilities.invokeLater`.
- **`WinUiUtilities.schedule(delayMillis, block)`**: Runs the block once on the UI thread after the given number of milliseconds. Calling `close()` on the returned `AutoCloseable` cancels it if it has not fired yet. The one-shot equivalent of `javax.swing.Timer`; the underlying implementation is a native `DispatcherQueueTimer`.

There is no synchronous counterpart to Swing's `invokeAndWait`.
If you need a result, use the coroutine integration (Section 5.4) or roll your own rendezvous with a `CountDownLatch` or similar.
However, waiting from on top of the UI thread deadlocks, so make sure the waiting side is always a worker thread.

### 5.3 Safe UI Updates from Worker Threads

The standard recipe for heavy work is "compute on a worker, and return only the result application to the UI thread."

```kotlin
loadButton.addActionListener {
    statusLabel.text = "Loading..."           // here we are on the UI thread
    Thread {
        val result = fetchFromServer()        // heavy work on the worker
        WinUiUtilities.invokeLater {
            statusLabel.text = result         // return to the UI thread to apply
        }
    }.start()
}
```

There are only two rules to keep.

- Do not touch `W*` objects from a worker thread. Even reads (property gets) violate the contract. If you need UI state, read it on the UI thread before starting the worker and pass the value along.
- Do not block the UI thread. This includes waiting via `Thread.join` or `Future.get`.

When the screen state can change before results arrive (changed search criteria, page navigation), you need a mechanism to suppress applying stale results.
The bundled Filer sample handles this by advancing a generation counter on every folder navigation and discarding results that come back via `invokeLater` with an old generation.
Use it as an implementation reference.

### 5.4 Coroutine Integration

Adding `winui4k-extension-coroutines` as a dependency makes `Dispatchers.WinUi` available.
It is the WinUI version of kotlinx-coroutines-swing's `Dispatchers.Swing`.

```kotlin
import com.appkitbox.winui4k.extension.coroutines.WinUi

loadButton.addActionListener {
    scope.launch(Dispatchers.WinUi) {
        statusLabel.text = "Loading..."
        val result = withContext(Dispatchers.IO) { fetchFromServer() }
        statusLabel.text = result             // automatically back on WinUi
    }
}
```

The key points are as follows.

- **It is also registered as `Dispatchers.Main`.** Through ServiceLoader registration of a `MainDispatcherFactory`, having this module on the classpath resolves `Dispatchers.Main` to `Dispatchers.WinUi`. Code written for Android or Swing that assumes `Dispatchers.Main` works as is.
- **`delay` runs on a native timer.** Delayed resumption is implemented with a one-shot `DispatcherQueueTimer`, so no thread is blocked.
- **`Dispatchers.WinUi.immediate` exists.** A variant that runs in place without re-dispatching when already on the UI thread, following the standard `MainCoroutineDispatcher` usage distinction.

Compared to the Thread approach of Section 5.3, cancellation (`Job.cancel`) and exception propagation are structured, which makes controls like discarding results on page navigation more concise.
It is the recommended choice for new code.

## Chapter 6: COM Reference Lifetime Management

In a [WinUI4K](https://github.com/nttr-tech/winui4k) app, two lifetime-management schemes with different premises coexist.
Native-side WinUI objects live and die by COM reference counting, while Kotlin-side `W*` wrappers live and die by the JVM's tracing GC.
This chapter explains the bridging mechanism and the operational conventions a user needs to know.
Implementation details and the comparison with CsWinRT are covered in Chapter 18.

### 6.1 The Mismatch between Reference Counting and Tracing GC

A COM object counts "the number of parties referencing me" itself: `AddRef` increments, `Release` decrements, and the moment it hits 0 the object frees itself.
The premise is that every user honors the convention "call `Release` when done."

The JVM's GC, on the other hand, does not count references; it judges liveness by reachability from roots.
Trying to connect these two runs into the following mismatches.

- **No release notification**: GC has no mechanism to detect "the moment the last reference to this object disappeared," so there is no way to know when `Release` should be called.
- **Release-thread constraint**: `Release` on XAML objects must be called from the UI thread (Chapter 5), but GC cleanup runs on dedicated threads.
- **Cycles across the boundary**: A cycle in which native and Kotlin reference each other cannot be collected by either scheme alone.

### 6.2 GC-Driven Automatic Release

[WinUI4K](https://github.com/nttr-tech/winui4k) resolves the first two mismatches with the following design.

**Map one wrapper to one unit of reference count.**
The COM reference acquired when a `W*` wrapper is created is returned via `Release` when GC detects the wrapper has become unreachable.
In other words, GC's reachability judgment is repurposed as the trigger for decrementing the reference count.
Detection uses `java.lang.ref.Cleaner` on Java 9 and later, and an equivalent homegrown mechanism built on `PhantomReference` on Java 8 (Chapter 18).

**Funnel `Release` onto the UI thread.**
The cleaner thread does not call `Release` directly; it sends release tasks through `ReleasePump` to the UI thread's message loop.
This always satisfies the apartment constraint, and structurally rules out an object being released out from under work executing on the UI thread.

What matters to users is that the wrapper's lifetime and the control's lifetime are separate things.
What is returned when a wrapper is collected is only "the one count the wrapper owned"; references the visual tree holds on the control remain intact.
A control currently displayed on screen does not disappear when its wrapper is GC'd.
Conversely, a control that was never added anywhere is destroyed on the native side at the same time its wrapper is collected.

In short, under normal use there is nothing to manage.
Create and discard `W*` objects with the same Swing-like ease, and the native side follows along and is released.

### 6.3 What Automatic Release Does Not Cover

The following two kinds are outside automatic release and hold their references indefinitely.

- **Window and Shell wrappers**: `WFrame`, `WAppWindow`, `WAppNotification`, `WJumpList`, and so on. They are few in number and live about as long as the app, so the judgment is that the complexity of automatic release is not worth it.
- **Shared infrastructure**: vtables, upcall stubs, cached HSTRINGs, factory statics. Held for the lifetime of the process.

Neither amounts to a problematic quantity in a normal app.

### 6.4 Cross-Language Cyclic References and the Practice of Removing Event Listeners

The third mismatch (cycles across the boundary) is not solved in [WinUI4K](https://github.com/nttr-tech/winui4k).
Concretely, the problematic shape is this.

A native control holds a reference to an event handler (a Kotlin-implemented COM object), that handler captures a Kotlin-side object, and that Kotlin object holds the wrapper of the original control.
Within this ring, the native side's reference count and the Kotlin side's reachability prop each other up, and neither is ever released.

CsWinRT solves this via an extension point of the .NET GC itself (`IReferenceTracker` integration), but the JVM's GC has no equivalent extension point, and it cannot be achieved in library code (Section 18.4).

Avoidance becomes a convention on the usage side.
That said, it is nothing special — just the ordinary etiquette of event subscription.

- Remove listeners that are no longer needed via remove-style methods (Chapter 8).
- Do not leave a listener that captures a wrapper registered on an object that outlives the control.

In designs that build and discard screens (page navigation and the like), the standard practice is to have a cleanup routine that removes listeners in bulk when the page is destroyed.

### 6.5 Tuning

There are three system properties related to lifetime management.

| Property | Values | Meaning |
|---|---|---|
| `winui4k.lifetime` | `cleaner` / `phantom` | Explicitly selects the cleanup mechanism. Default is auto-selected by Java version |
| `winui4k.gcThreshold` | Reference count (integer) | Requests `System.gc()` whenever the number of live native references exceeds the threshold. Disabled by default |
| `winui4k.ffi` | `panama` / `jna` / `jnr` | Explicitly selects the FFI backend (Chapter 7) |

`winui4k.gcThreshold` is a mitigation for the problem that GC cannot observe native-side memory volume (the JVM has no API corresponding to .NET's `GC.AddMemoryPressure`).
Since it only looks at the "count" of references, it is positioned as opt-in insurance; consider it for apps that rebuild large UIs on short cycles (combining it with `-XX:+ExplicitGCInvokesConcurrent` is recommended).

This property is also useful for verifying that automatic release works.
Launch Gallery with a low threshold like `-Dwinui4k.gcThreshold=200` and switch back and forth between category pages a few times, and the GC-driven release path is forced to fire frequently.
If the UI stays operable, no release failures appear on standard error, and the process exits with code 0, then the entire create-release-shutdown path is sound.

## Chapter 7: FFI Backends

Native calls in [WinUI4K](https://github.com/nttr-tech/winui4k) are handled by pluggable FFI backends.
This chapter explains the characteristics of the three implementations, the selection mechanism, and the cautions for running on Java 8.

### 7.1 Characteristics and Supported Environments of the Three Implementations

| Backend | Implementation basis | Java | Architectures | Priority |
|---|---|---|---|---|
| `winui4k-ffi-panama` | Panama (`java.lang.foreign`, the FFM API) | 22 or later | Whatever the JDK supports (x64 / ARM64) | 100 (default) |
| `winui4k-ffi-jna` | JNA | 8 or later | x64 only | 50 |
| `winui4k-ffi-jnr` | JNR (jffi = libffi) | 8 or later | x86 / x64 / ARM64 | 40 |

The three implementations implement the same SPI (`FfiBackend` in `internal.ffi.api`) and are functionally equivalent.
The differences lie in supported environments and call cost.

- **Panama** is the standard API finalized in Java 22 and brings in no additional native libraries. On Java 22 and later it is the default.
- **JNA** is x64-only because it hand-converts struct-by-value passing per the Windows x64 calling convention. It cannot be used on ARM64.
- **JNR** works on all of x86 / x64 / ARM64 because the low-level libffi layer handles calling-convention conversion. On Java 8-21 ARM64 environments it is the only option.

### 7.2 Runtime Selection via ServiceLoader and Explicit Specification

Place one or more backends on the runtime classpath; the choice is fixed at the first native call, in the following order of precedence.

1. Explicit specification via `WinUiUtilities.setFfiBackend(...)` (callable only before the first FFI use)
2. The system property `-Dwinui4k.ffi=panama|jna|jnr`
3. Among those discovered by ServiceLoader, the one that is available (`isAvailable`) with the highest priority

The automatic selection of option 3 is normally fine.
On Java 22 and later Panama is chosen; below that, JNA (x64) or JNR.
Even if the Panama module is mixed into a Java 8 classpath, the failure to load Java 22 bytecode is detected and the backend is skipped, so `winui4k-all` can serve as a common dependency across all versions.

Explicit specification is useful for isolating behavioral differences (does a phenomenon seen with Panama also occur with JNA?) and for validating a specific backend.

### 7.3 Cautions for Running on Java 8

- **Backend choice**: On x64, either JNA or JNR works (the default is JNA by priority). On ARM64, JNR is the only choice.
- **`--enable-native-access` is not needed**: This option is for Panama on Java 22 and later; do not specify it on Java 8-21.
- **When building a fat JAR**: FFI backends are discovered via ServiceLoader, so configuration that merges the same-named `META-INF/services` files (`mergeServiceFiles()` with the Shadow plugin) is mandatory. Without merging, some backends become undiscoverable.
- **Means of verification**: The repository has `runJna` / `runJnr` tasks that launch Gallery with JDK 8 + JNA / JNR, and the foojay resolver fetches the required JDKs automatically (Section 2.6). For your own app too, it is recommended to add a startup check on the minimum supported Java version to CI (Chapter 13).

Note that the lifetime-management underpinnings also switch by Java version (Java 8 uses the `PhantomReference` approach), but this happens automatically and users need not think about it (Chapter 18).

# Part III: Practical Guide

## Chapter 8: Event Handling

### 8.1 Registering and Removing Listeners

The shape of the event API follows Swing.
Register with `addXxxListener`, remove with `removeXxxListener`.
Listener types are Kotlin function types; no dedicated listener interfaces are defined.

```kotlin
val onClick: () -> Unit = { println("clicked") }
button.addActionListener(onClick)
// ...
button.removeActionListener(onClick)
```

- Listeners are always invoked on the UI thread, so they can update the UI directly (Chapter 5).
- The same listener can be added multiple times; each remove removes one registration (the most recently added).
- Removal matching uses **reference equality**. Writing a lambda in place like `removeActionListener { ... }` cannot remove anything, because it is a different instance from the one registered. Keep listeners you intend to remove in a variable, as in the example above, and pass the same reference.

In addition to control-specific events (`WButton`'s ActionListener, `WTextField`'s TextChangedListener, and so on), the base `WComponent` has listeners common to all controls (`addSizeChangedListener`, `addLoadedListener`, `addActualThemeChangedListener`).

### 8.2 What a Delegate Really Is

Inside `addActionListener`, the WinRT event-subscription protocol runs exactly as designed.
Knowing the mechanism lets you understand not just the listener-management conventions (Section 8.3) but why they are necessary.

1. A COM object (the delegate) wrapping the Kotlin lambda is constructed with `KComObject`. This is an implementation of a WinRT delegate (`RoutedEventHandler` or `TypedEventHandler`) with a single `Invoke` method — a genuine COM object that native code can call back (Section 16.4).
2. The control's event-registration method, such as `add_Click`, is called. It returns an `EventRegistrationToken` (a 64-bit integer), which becomes the key for unsubscribing.
3. The `W*` class keeps a listener-to-token map. `removeActionListener` looks up the token in this map and passes it to `remove_Click` to unsubscribe.

Once registration completes, the reference to the delegate is held by the control's event table.
In other words, **the Kotlin lambda's survival is in the hands of the native-side subscription**.
Removing the listener detaches the native-side reference, and the lambda becomes GC-eligible again.

### 8.3 Listener-Management Patterns That Avoid Leaks

As stated in Chapter 6, cyclic references across the language boundary are not collected automatically.
Event listeners are the classic ingredient of that cycle.

```
Native control ──(event table)──> delegate (Kotlin lambda)
        ↑                                       │ captures
        └──(COM reference via wrapper)── Kotlin object <┘
```

The practical guidelines are as follows.

- **Listeners with the same lifetime as their control need no removal.** If, as with a button and its click listener, all the listener captures is the screen it belongs to, discarding the whole screen makes the entire cycle unnecessary. Strictly speaking this form of cycle is a leak, since a tracing GC cannot collect it, but the amounts are small, and it only becomes a problem when screens are created and destroyed in large volume.
- **Listeners that span objects of different lifetimes must be removed.** Registering screen-side listeners on a single app-wide model, or registering a listener that captures a short-lived dialog on a long-lived control — these shapes are dangerous. Remove them explicitly with the remove-style methods.
- **Gather cleanup in one place.** The standard practice is a `dispose`-like routine called when a page or dialog is destroyed, removing all registered listeners there in one sweep. Writing registration and removal as a pair makes missed removals easier to spot in review.

## Chapter 9: Layout

There are three means of arranging screens in [WinUI4K](https://github.com/nttr-tech/winui4k):
using WinUI panels directly (Section 9.1), Swing-style layout managers (Section 9.2), and the MigLayout extension (Section 9.3).
As a rule of thumb: WinUI panels for simple vertical/horizontal stacking, layout managers for ports from Swing or custom layouts, and MigLayout for complex form-style arrangements.

### 9.1 How Placement with WinUI Panels Works

WinUI layout follows the model of "the parent panel places its children," and the panel's kind determines the placement rules.
[WinUI4K](https://github.com/nttr-tech/winui4k) wraps the major panels.

**WPanel (StackPanel).**
Lines children up in a single column or row.
`WFrame`'s default content area (`contentPane`) is this `WPanel`, which is why things added with `frame.add` stack vertically in order.

```kotlin
val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
row.add(WLabel("Name"))
row.add(WTextField())
frame.add(row)
```

**WGrid (Grid).**
Define rows and columns, then place children at specified cell positions.
Row and column sizes are specified with `GridLength`, which comes in three kinds: `AUTO` (fit to content), `pixel(value)` (fixed), and `star(weight)` (distribute remaining space by weight).

```kotlin
val grid = WGrid(rowSpacing = 8.0, columnSpacing = 8.0)
grid.addColumn(GridLength.AUTO)          // label column sized to content
grid.addColumn(GridLength.star())        // input column takes the rest
grid.addRow(GridLength.AUTO)
grid.add(WLabel("Name"), row = 0, column = 0)
grid.add(WTextField(), row = 0, column = 1)
```

Beyond these there are `WCanvas` (absolute coordinates), `WRelativePanel`, `WVariableSizedWrapGrid`, and more.
To replace the root composition entirely, swap out the `contentPane` itself with `frame.setContentPane(component)`.
A real example is Gallery's layout, which puts a `WGrid` of "title-bar row + content row" at the root.

### 9.2 Swing-Style Layout Managers

As the counterpart of Swing's `LayoutManager`, there is the `WLayoutManager` interface and its host environment, `WLayoutPanel`.

```kotlin
interface WLayoutManager {
    fun addLayoutComponent(component: WComponent, constraints: Any?)
    fun removeLayoutComponent(component: WComponent)
    fun preferredLayoutSize(parent: WLayoutPanel): WSize
    fun layoutContainer(parent: WLayoutPanel)
}
```

`WLayoutPanel` is built on WinUI's Canvas (a panel that does not auto-place children); all measurement and placement computation happens on the Kotlin side, and the results are applied as the children's coordinates and sizes.
The layout flow is as follows.

1. WinUI's synchronous layout (`UpdateLayout`) finalizes each child's desired size (DesiredSize)
2. The layout target area is determined (priority: the user's explicit size > the parent's allocation > the measured size > `preferredLayoutSize`)
3. `layoutContainer` computes each child's position and size within that area and applies them via `setBounds`

`WBorderLayout` (five regions: NORTH / SOUTH / EAST / WEST / CENTER) ships as the standard implementation.

```kotlin
val panel = WLayoutPanel(WBorderLayout())
panel.add(toolbar, WBorderLayout.Constraint.NORTH)
panel.add(content, WBorderLayout.Constraint.CENTER)
panel.add(statusBar, WBorderLayout.Constraint.SOUTH)
```

After you yourself cause children to be added/removed or sizes to change, request a re-layout with `revalidate()` (multiple requests within the same UI turn are coalesced into one).
If you need a custom layout, implement `WLayoutManager` using the `WBorderLayout` implementation (about 100 lines) as a template.

### 9.3 Placement with the MigLayout Extension

`winui4k-extension-miglayout` is an adapter that places `W*` controls using the constraint strings of the MigLayout layout library.
The implementation is `MigLayoutManager`, a `WLayoutManager` implementation, and grid computation is delegated wholesale to miglayout-core (the 5.3 series, the last Java 8-compatible line).

```kotlin
val form = WLayoutPanel(MigLayoutManager("wrap 2", "[right][grow, fill]"))
form.add(WLabel("Name"))
form.add(WTextField())
form.add(WLabel("Mail"))
form.add(WTextField())
form.add(WButton("Submit"), "skip, right")
```

The constraint grammar is the same as upstream MigLayout (layout constraints, column constraints, row constraints, component constraints).
Forms that have used MigLayout with Swing or SWT can bring their constraint strings over almost unchanged.
The bundled Form with MigLayout sample (Section 2.6) is a working example.

There is one WinUI-specific caution.
WinUI has no "minimum size" measurement, so this adapter treats a component's minimum size as equal to its desired size.
When shrinking the window, components you want to shrink below their desired size need an explicit constraint such as `"width 0::"`.

### 9.4 DPI Scaling and Coordinate Systems

[WinUI4K](https://github.com/nttr-tech/winui4k) declares Per-Monitor v2 DPI awareness for the process at startup (Section 4.1).
Tracking per-monitor scaling (100% / 150% / 200%, etc.) is handled by WinUI; no app-side work is needed.

There are only two coordinate units to remember.

- **Everything layout-related is in DIPs (logical pixels, Double)**: The width, height, and margins of `W*` components, `WSize` / `WInsets`, and layout-manager computations all use this unit. On a 150%-scaled monitor, 1 DIP is rendered as 1.5 physical pixels, but you never think about it in code.
- **`WAppWindow`'s position and size are in physical pixels (integers)**: The window's own on-screen position (`position: WPoint`) and size (`size: WDimension`) use the OS window-management unit, i.e., physical pixels.

If you ever feel "I set the window to 800×600 but the controls don't look right," suspect a mix-up between these two units.

## Chapter 10: Asynchronous Work and UI Updates

### 10.1 UI Update Patterns during Network Calls and Heavy Work

The basic form is the one shown in Chapter 5: "compute on a worker, apply via `invokeLater` (or a coroutine)."
In practice, two more elements come into play.

**Showing progress.**
To indicate that work is in progress, update the UI before starting the worker, and restore it on completion.
The staples are showing a `WProgressRing` or `WProgressBar` (indeterminate mode) and setting the button to `isEnabled = false`.

```kotlin
searchButton.addActionListener {
    searchButton.isEnabled = false
    progressRing.isActive = true
    scope.launch(Dispatchers.WinUi) {
        try {
            val results = withContext(Dispatchers.IO) { search(queryField.text) }
            resultList.setItems(results)
        } finally {
            progressRing.isActive = false
            searchButton.isEnabled = true
        }
    }
}
```

**Discarding stale results.**
For operations where conditions can change before results arrive (incremental search, folder navigation), last-writer-wins control is needed.
With coroutines, the concise approach is to `cancel` the previous `Job` before launching the new work.
When building on raw threads, the Filer sample's generation-counter approach (advance the counter on every operation, discard at application time if the generation is old) is a useful reference.

### 10.2 Living with WinRT's Asynchronous APIs

Many WinRT APIs are asynchronous methods that return `IAsyncOperation<T>` / `IAsyncAction`.
[WinUI4K](https://github.com/nttr-tech/winui4k) processes these in its internal `Async` machinery and, **as a rule, exposes no asynchronous types in the public API**.
What users see comes in two forms.

- **Synchronous APIs that block until completion**: `WJumpList.load()` and `save()` wait internally for the asynchronous API to complete before returning. The wait uses `CoWaitForMultipleObjects`, which dispatches incoming COM calls even while waiting on the UI thread, avoiding deadlock (10-second timeout). This approach is used only for operations that finish quickly.
- **APIs that take a callback**: Like `WWebView.executeScript(script) { result -> ... }`, the completion handler is passed as an argument. Operations whose completion itself requires the UI's message processing to keep going — like script execution — would structurally deadlock if blocked. Callbacks are invoked on the UI thread.

When extending the wrapped surface yourself (Chapter 21), follow the same split.
The criterion is: "synchronize short, UI-independent completion waits; use callbacks for operations that need the UI to keep making progress."

## Chapter 11: Themes and Appearance

### 11.1 Fluent Design and Default Styles

The reason [WinUI4K](https://github.com/nttr-tech/winui4k) controls appear with the Fluent Design look from the start is that `XamlControlsResources` (the collection of default styles for all controls) is automatically added to `Application.Resources` at startup (Section 4.1).
No app-side work is involved.

On top of that, the main appearance-related APIs are the following.

- **Accent color**: Setting `isAccent = true` on a `WButton` turns it into an emphasized button using the OS accent color (`AccentButtonStyle`). Use it for things like a dialog's default action.
- **Backdrop material**: `WFrame.systemBackdrop` applies a Windows 11 material to the window background. Four choices: `MICA` (a faint translucency of the desktop wallpaper), `MICA_ALT`, `ACRYLIC` (frosted glass), and `NONE` (default). Gallery and Filer use `MICA`.
- **Extending into the title bar**: `WFrame.extendsContentIntoTitleBar = true` extends the content area into the title bar, and `setTitleBar(component)` designates the drag region. You can build layouts like Gallery's, which embeds a search box in the title bar.

### 11.2 Dark Mode Support

Themes are controlled with the three-valued `ElementTheme` (`DEFAULT` / `LIGHT` / `DARK`).

- **`WComponent.requestedTheme`**: Specifies the theme for an element and its descendants. `DEFAULT` means following the OS setting. Set it on the root element (the contentPane, or the component passed to `setContentPane`) and it effectively becomes an app-wide theme switch.
- **`WComponent.actualTheme`**: Returns the actually resolved theme (`LIGHT` or `DARK`). Used to find out which one the OS currently is when `requestedTheme = DEFAULT`.
- **`addActualThemeChangedListener`**: Notifies at the moment the theme switches, e.g., due to a change in OS settings.

The coloring of standard controls follows theme switches automatically.
The only thing the app must handle itself is **colors you hard-coded yourself**.
Gallery handles this by "defining color properties that yield different values for dark/light and repainting the currently displayed page in `addActualThemeChangedListener`," serving as an implementation example for apps with custom color schemes.
A settings UI that lets the user pick the theme (Light / Dark / Use system setting) is also exemplified by Gallery's SettingsPage.

### 11.3 How Resource Resolution Works

This section is background knowledge for when you encounter theme-related errors.

WinUI's styles and themes sit on top of the XAML resource system, and the default theme resources are normally resolved from `resources.pri`, a resource file inside the app package.
But a [WinUI4K](https://github.com/nttr-tech/winui4k) app is unpackaged (a configuration without MSIX), and the executable is java.exe.
No `resources.pri` exists next to it, so in the raw state XAML cannot find the default theme resources (`ms-appx:///Microsoft.UI.Xaml/Themes/themeresources.xaml`) and creating `XamlControlsResources` fails.

[WinUI4K](https://github.com/nttr-tech/winui4k) solves this via the official extension point, the `Application.ResourceManagerRequested` event.
At startup, it registers on this event a handler that returns an MRT Core `ResourceManager` reading the `resources.pri` in the Windows App SDK runtime package (Section 4.1).
If you see errors like "Cannot locate resource from 'ms-appx:...'", suspect a code path that touches XAML resources before this initialization runs.

For the same reason, access to `Application.Resources` fails before `OnLaunched` (E_UNEXPECTED).
User code always runs after `OnLaunched`, so this is normally invisible, but when extending the library internals (Chapter 21) it becomes a binding constraint.

## Chapter 12: OS Integration and Advanced Use

### 12.1 Shell Integration

There are three families of APIs for integrating with the Windows shell (taskbar, notification center).

**Toast notifications (WAppNotification).**
Notification content is assembled with the `WAppNotification` builder API and shown with `WAppNotificationManager`.

```kotlin
WAppNotificationManager.register()
WAppNotificationManager.addNotificationInvokedListener { args ->
    // on notification click; args holds the values embedded via addArgument
}
val notification = WAppNotification("Build finished")
    .addText("winui4k: BUILD SUCCESSFUL")
    .addButton("Open log", "action=openLog")
WAppNotificationManager.show(notification)
```

Clicking a notification can involve relaunching the app, so complete `register()` and the Invoked-listener registration at startup.
Availability can be checked with `WAppNotificationManager.isSupported`, and whether the user has disabled notifications with `setting`.

**Badges (WBadgeNotification).**
Numeric badges (`setCount`) and glyph badges (`setGlyph`) on the taskbar icon. Used for things like unread counts.

**Jump lists (WJumpList).**
The right-click menu of the taskbar icon.
Fetch the current contents with `WJumpList.load()`, add `WJumpListItem`s, and apply with `save()`.
**Jump lists are exclusive to apps with a package identity**; in unpackaged execution (a plain `java -jar`), `isSupported` is false. Always check before use.

### 12.2 Embedding WebView2

`WWebView` is a wrapper over WebView2, the Microsoft Edge-based browser control.
It lets you embed web-based screens and document viewing into a native app.
The required WebView2 runtime ships standard with Windows 11, and the SDK-side DLL is bundled in the winui4k JAR, so no additional setup is needed.

```kotlin
val webView = WWebView("https://example.com")
webView.addNavigationCompletedListener { success, status ->
    if (!success) statusLabel.text = "Load failed: $status"
}
frame.setContentPane(webView)
```

The properties to be aware of are as follows.

- **Initialization is asynchronous.** Starting the browser process takes time, so setting `source` or calling `navigateToString` is applied in order after initialization completes. If you want to use operations that require CoreWebView2 (such as `documentTitle`) before initialization, start it with `ensureCoreWebView2()` and wait for completion with `addCoreWebView2InitializedListener`.
- **JavaScript interop**: Run scripts with `executeScript(script) { result -> ... }`, and do bidirectional messaging with the page via `postWebMessageAsJson` / `addWebMessageReceivedListener`.
- **Navigation control**: Returning false from `addNavigationStartingListener` cancels the navigation. Useful for purposes like blocking external links.
- **User data folder**: WebView2 needs somewhere to write profile data. If unspecified, a writable location (under LOCALAPPDATA) is set automatically, so normally you need not think about it.

### 12.3 Calling Unwrapped WinUI APIs

What the `W*` classes wrap is a subset of WinUI.
When a feature you want is unwrapped, here are the options in order of recommendation.

**Fork the repository and add a wrapper.**
This is the main line.
The internals of [WinUI4K](https://github.com/nttr-tech/winui4k) are designed to be extended by the mechanical procedure of "extract ABI constants from winmd, place the constants in `*Interop`, call the slot via `ComPtr`"; Chapter 17 and Section 21.3 give the procedure.
Adding a single property to an existing control takes only a few dozen lines of change.
For additions with general utility, consider a PR to the upstream project (Section 21.4).

**Call the internal layers directly.**
The internal layers (`ComPtr`, `*Interop`, etc.) have Kotlin `internal` visibility and are invisible to ordinary Kotlin code.
However, since `internal` is public in JVM bytecode, code written in Java (or workarounds such as `@Suppress`) can technically call them.
This works for temporary experiments where you want to avoid a fork, but the internal layers are territory with no compatibility promises. For permanent code, choose the fork of the previous item.

**Request via an Issue.**
If you lack the capacity to implement it, you can file a wrapping request on GitHub Issues (Section 21.4).

# Part IV: Quality and Distribution

## Chapter 13: Testing

### 13.1 Test Strategy

[WinUI4K](https://github.com/nttr-tech/winui4k)'s own tests are E2E: they actually launch WinUI windows and operate the controls.
WinUI is not substituted with mocks or stubs.
For a library that crosses three boundaries — FFI, the COM ABI, and the real WinUI — if any single assumption at any boundary is wrong the whole thing breaks, so there is no trustworthy method other than "verify against the real thing."

Tests for apps using [WinUI4K](https://github.com/nttr-tech/winui4k) can be written with the same toolkit.
However, E2E requires a real desktop session and runs comparatively slowly, so for the app side the following division of labor is recommended.

- **Separate business logic from the UI and verify it with ordinary unit tests.** This should be the bulk.
- **Verify the UI wiring (event → state → display) thinly with E2E.** The UiTestHarness approach of the next section works for this.

### 13.2 Writing E2E Tests with UiTestHarness

The first constraint you hit in E2E testing is the one stated in Chapter 4: "the message loop can only be started once per JVM process."
You cannot open and close a window per test.

`UiTestHarness`, winui4k's test infrastructure (winui4k/src/test), solves this with the following design.

- Lazily create a single shared `WFrame` and reuse it across all test classes. The window is shown with `activate = false`, so it does not steal focus from anyone using the PC while tests run.
- Controls under test are swapped in and out of this shared frame via `attach` / `detach`. When exercising events that fire only after template application (such as TextChanged), use `attachAndAwaitLoaded` to wait for Loaded before operating.
- The `W*` API is under the UI-thread contract (Chapter 5), so operations from test code are sent to the UI thread via `onUiThread { }` / the result-returning `onUiThreadGet { }`.
- Once after all tests finish, the shared window is closed and the message loop's shutdown is awaited (if the process dies without waiting for shutdown, it collides with COM's deferred releases and crashes).

The test framework is Kotest (FunSpec style).
An actual test looks like this.

```kotlin
class WButtonTest : FunSpec() {
    init {
        test("the label passed to the constructor can be read back from text") {
            onUiThreadGet { WButton("Run").text } shouldBe "Run"
        }
    }
}
```

The basic form is "create and operate on the UI thread, extract the result, assert on the test thread."
Run tests with the following commands.

```powershell
.\gradlew :winui4k:test                        # all tests
.\gradlew :winui4k:test --tests "WButtonTest"  # single class (no wildcards; use the simple or fully qualified name)
```

### 13.3 Running in CI

E2E tests require a real desktop session and do not work in headless environments.
Fortunately, GitHub Actions Windows runners (`windows-latest`) provide an environment equivalent to an interactive session, so WinUI can be launched without extra contrivances like virtual displays.
winui4k's own CI (`.github/workflows/build.yml`) is a working example, structured as follows.

1. Set up JDK 25 (the JDK 8 / 9 / 22 used for tests are fetched automatically by Gradle's foojay resolver)
2. Fetch the Windows App SDK runtime installers with `.\gradlew :winui4k:downloadInstallers` and install with `--quiet --force`
3. Run `.\gradlew build testOnAllJavaVersions`

`testOnAllJavaVersions` is a task that runs the same tests in sequence on the JDK 8 / 9 / 22 / 25 toolchains.
The reason for four versions is that the boundaries of the runtime switching for FFI and lifetime management (Chapters 7 and 18) sit at Java 9 and 22, and both sides of each boundary are covered.
JNA is auto-selected on JDK 8 / 9 and Panama on 22 and later, so the backend combinations are verified at the same time.

ARM64 is verified in a separate job on the `windows-11-arm` runner (JDK 25 only, since no Windows ARM64 builds of JDK 8 / 9 exist; there are other differences too, such as using Microsoft Build of OpenJDK instead of Temurin).
When setting up CI for your own app, this build.yml also works as a starting point.

## Chapter 14: Debugging and Troubleshooting

### 14.1 Diagnosis with HRESULT and IRestrictedErrorInfo

Error handling in [WinUI4K](https://github.com/nttr-tech/winui4k) comes in exactly one kind.
When a COM call fails (a negative HRESULT), a `WindowsRuntimeException` is thrown.

```
WindowsRuntimeException: XamlControlsResources.Append failed: HRESULT=0x802B000A (detail message)
```

Read it as follows.

- **Which operation failed**: The operation name at the head of the message. It indicates which internal COM call it was.
- **The HRESULT code**: A 32-bit value starting with `0x`. It is a Windows-wide error code you can look up as is in Microsoft's documentation or an error-code search. Frequent values are summarized in Section 2.5.
- **The detail message**: Attached only when it could be obtained. WinRT has a mechanism (`IRestrictedErrorInfo`) that carries a human-readable error detail alongside the HRESULT, and [WinUI4K](https://github.com/nttr-tech/winui4k) queries it when constructing the exception, concatenating the error description the XAML runtime left behind (such as "which resource could not be found") onto the message.

The code can also be read programmatically from the exception's `hresult` property.
There is no further typed error hierarchy (no tree of exception classes).
Judging "which HRESULTs are recoverable" is the app's responsibility.

### 14.2 Trouble Index by Symptom

Startup troubles are collected in Section 2.5.
Here we cover symptoms during execution.

| Symptom | Likely cause | Remedy |
|---|---|---|
| The UI hangs (freezes) | Long-running work or blocking waits on the UI thread | Offload heavy work to workers (Chapters 5 and 10). Identify where the `WinUI4K-UI` thread is stuck with a thread dump |
| Sudden crash, JVM abort | Touching the `W*` API from a worker thread | Route all UI operations through `invokeLater`. Plant `check(WinUiUtilities.isDispatchThread)` at suspicious spots to pinpoint |
| Crash or abort at shutdown | Bypassing the proper shutdown path (Section 4.3) with `System.exit` or the like | Exit via `WinUiUtilities.exit()` |
| Controls are not rendered | Failure applying the default styles (`XamlControlsResources`) | Check the HRESULT on the console (Sections 2.5 and 11.3) |
| TabView: adding to `TabItems` does not add tabs | You cached an IVector obtained before display. TabView swaps out the underlying collection at display time, so additions to the old instance never reach the screen | Re-fetch the collection on every operation. In wrapping code, avoid caching IVectors as a rule |
| Some colors remain after switching dark mode | Hard-coded colors do not follow automatically | Repaint in `addActualThemeChangedListener` (Section 11.2) |
| Memory usage grows monotonically | Cyclic references from missed listener removal (Chapters 6 and 8), or native references awaiting release | Suspect missed removes first. If it depends on GC frequency, isolate with `-Dwinui4k.gcThreshold` (Section 6.5) |

### 14.3 A Collection of Anti-Patterns

Here, preemptively, are the classics that get flagged in review.
For each, the "why it is bad" is in the chapters already covered.

- **Blocking work on the UI thread**: Network calls, file I/O, `Thread.sleep`, `Future.get` inside a listener. The entire UI stops (Chapter 5).
- **`W*` operations from worker threads**: Not just writes — reads also violate the contract. It can break without an exception, so "it happened to work" is easily mistaken for safe (Chapter 5).
- **removeListener with an in-place lambda**: Matching is by reference equality, so it cannot remove anything. Keep listeners you plan to remove in a variable (Section 8.1).
- **Forgetting to remove listeners that span lifetimes**: Letting a long-lived object capture a short-lived screen leaves a cross-language cycle in place forever (Sections 6.4 and 8.3).
- **Caching the underlying instance of collection properties**: As in the TabView example, some collections get their instance swapped out by WinUI. Re-fetching each time is safe (Section 14.2).
- **Immediate termination via `System.exit`**: It skips the shutdown sequence of stopping releases → `RoUninitialize`, inviting crashes during shutdown. Use `WinUiUtilities.exit()` (Section 4.3).
- **Creating UI elements outside `invokeLater`**: `W*` objects make COM calls at constructor time, so creation too must happen on the UI thread (Chapter 5).

## Chapter 15: Packaging and Distribution

There are two distribution forms for a [WinUI4K](https://github.com/nttr-tech/winui4k) app:
**JAR distribution** for environments with a JVM, and a **JRE-bundling installer via jpackage** for end users.
In both cases, factor in the Windows App SDK runtime setup (Section 2.4).

### 15.1 App Distribution via jpackage

Distribution that does not ask end users to prepare a JVM is done with jpackage, which ships with the JDK.
The Gallery sample's `packageExe` task is the working example; the key points are as follows.

- **Narrow the input to a single fat JAR**: Build a fat JAR bundling the app and all dependencies, and pass a directory containing only that to jpackage's `--input` (`--input` bundles the entire directory). When building the fat JAR, the three FFI backends have same-named `META-INF/services` files, so **merging service files (the Shadow plugin's `mergeServiceFiles()`) is mandatory**.
- **Add `--java-options --enable-native-access=ALL-UNNAMED`**: The bundled JRE will be Java 22 or later, so the Panama backend is selected and this option is required (Section 2.2).
- **Pin `--win-upgrade-uuid`**: The ID that makes over-the-top installs work correctly. Gallery derives it deterministically from the app ID so it does not change from build to build.
- **`--type exe` requires the WiX Toolset**: Gallery's build downloads and uses WiX 3.14 automatically.

This yields "an .exe installable by double-click, no JVM required."
For Gallery you can actually generate it with `.\gradlew :winui4k-sample-gallery:packageExe`, so copying that task definition is the fast path to a template for your own app.

For JAR distribution, build a fat JAR and include instructions to launch with `java --enable-native-access=ALL-UNNAMED -jar app.jar` (Java 22 or later) or `java -jar app.jar` (Java 8-21, when using JNA / JNR).

### 15.2 Bundling the Runtime Installer

On machines without the Windows App SDK runtime, the default behavior is Microsoft's download-prompt dialog (Section 2.4).
If you do not want end users to perform that step, bundle the runtime installer with your distribution.

1. Fetch the installers for all three architectures with `.\gradlew :winui4k:downloadInstallers` (about 104 MB each)
2. Place the `WindowsAppRuntimeInstall-<arch>.exe` for the target architecture in the app's working directory (or the directory specified by `winui4k.installer.dir`)

With just this, a silent install runs on first launch and the app starts without user interaction.
The Windows App SDK license permits redistribution of the installer, so there is no legal obstacle to bundling (Section 1.4).
Note that the runtime is a per-machine install, so this step does not run on second and subsequent launches.

### 15.3 Distribution Size and Startup Time

The distribution size breaks down into roughly three parts.

- **The app itself (fat JAR)**: winui4k plus a backend and app code — under a few tens of MB. Includes the bootstrap DLLs embedded in the JAR (all three architectures).
- **The bundled JRE (for jpackage)**: A few tens of MB. There is room for reduction via module trimming with jlink.
- **The runtime installer (if bundled)**: About 104 MB. The largest element, but you can also choose not to bundle it and rely on the prompt dialog.

Compared to the Electron approach, which must always bundle Chromium and Node.js, the baseline is smaller, and the composition gives you choices about what to bundle.
Not bundling a rendering engine also means a smaller baseline for runtime memory usage (Section 1.4).

Startup time is the sum of "JVM startup + Windows App SDK initialization + XAML initialization," disadvantaged relative to C#-built WinUI apps by the JVM startup portion.
The standard way to improve perceived startup is to minimize the initialization block passed to `invokeLater` so the first window appears quickly, deferring the rest of initialization until after display.

# Part V: Internals

Part V explains the machinery itself by which [WinUI4K](https://github.com/nttr-tech/winui4k) "runs WinUI without a bridge DLL."
Use it as a map when stepping into the internals while debugging, when forking and taking over maintenance (Section 1.4), or when extending the wrapped surface (Chapter 21).
The COM basics of Section 3.3 and Chapter 6 are assumed.

## Chapter 16: Handling the COM ABI Directly from the JVM

WinUI objects are COM objects within the process, and their calling convention is fixed at the binary level.
This chapter looks at the six building blocks for satisfying that convention with nothing but the JVM's FFI, from the bottom up.

### 16.1 vtable Calls and ComPtr

The foundation of everything is "two levels of indirection from interface pointer to function pointer."
`ComPtr` (internal/com) is the minimal unit encapsulating this: it wraps a raw address and calls methods by the following procedure.

1. Read the first 8 bytes the pointer points to → the vtable's address
2. Read `vtable + slot number × 8` → the function pointer of the target method
3. Call that function pointer via an FFI downcall. The first argument is always the object's own pointer (equivalent to C++'s this)

The call's signature is expressed by a `CallDescriptor` (the sequence of return and argument types; backend-independent vocabulary from `internal.ffi.api`).
`ComPtr.call(slot, args...)` infers the descriptor from the arguments' Kotlin types (Ptr / Int / Long / Double / struct) and checks the returned HRESULT.
Additionally it provides `getPtr` / `getInt` / `getBool` and friends for COM's frequent "receive the result through an out pointer" pattern, and the three IUnknown methods (`queryInterface` / `addRef` / `release`, slots 0 / 1 / 2).

Rebuilding the descriptor per call would be wasteful, so for calls with scalar-only arguments the sequence of argument types is encoded into an integer, and downcall handles are cached and reused.
It is important that `ComPtr` itself manages no lifetime; the responsibility for release falls to the machinery of Chapter 18.

### 16.2 HSTRING and String Conversion

Conversion between WinRT's string type HSTRING and Java's String is handled by `Hstring` (internal/winrt).
The implementation is calls to combase.dll's `WindowsCreateString` / `WindowsGetStringRawBuffer` / `WindowsDeleteString`, converting back and forth through UTF-16 buffers.

There are two refinements.

- **A deliberately leaked cache**: The runtime class names passed to `RoGetActivationFactory` (`"Microsoft.UI.Xaml.Controls.Button"` and the like) are a small set of fixed strings used repeatedly, so `ofCached` caches the HSTRINGs for the process lifetime. This is one instance of Chapter 6's "shared infrastructure is never released."
- **Duplication when ownership transfers**: Handing a cached HSTRING to an output destination governed by the "callee frees it" convention would leave the cache dangling. On such paths (like the `GetRuntimeClassName` response in an upcall), the string is duplicated with `WindowsDuplicateString` before being handed over. An example of COM's ownership conventions (Chapter 6) applying to strings just the same.

### 16.3 RoGetActivationFactory and Object Creation

WinRT object creation is a two-step process: "get a factory from the class name, and have the factory create the object."
`Activation` (internal/winrt) organizes this into three forms.

- **`factory(runtimeClass, iid)`**: Obtains the activation factory via `RoGetActivationFactory`. This same factory is also used for calling static methods (`Application.Start` and the like).
- **`activate(runtimeClass, iid)`**: The default-constructor equivalent. Creates an instance via the factory's `ActivateInstance` and returns it QI'd to the target interface. The intermediate pointers obtained along the way (the factory and the IInspectable) are reliably released internally. This "leaked intermediate reference" is the leak most easily made by hand, and is the reason the two-argument form exists.
- **`composeDefault(runtimeClass, factoryIid)`**: For inheritable (composable) classes. Calls the factory's `CreateInstance(outer, &inner, &instance)` with outer = null. Passing a real object as outer produces COM aggregation (Section 16.5).

Which form each W* class uses is determined by the class's kind recorded in winmd (activatable / composable) (Chapter 17).

### 16.4 COM Object Implementation via Upcalls

So far the direction has been JVM calling native (downcalls).
`KComObject` (internal/winrt) is what builds **Kotlin-implemented COM objects that native code calls back** (CCWs) — event handlers, `Application` overrides, and the like.

A COM object, seen from outside, is "a memory block whose first member is a pointer to a vtable," so in principle assembling the following makes a real one.

1. Convert Kotlin lambdas into FFI upcall stubs (function pointers callable from native code)
2. Write a vtable of the stubs' function pointers into native memory
3. Allocate an object body whose first member is the pointer to the vtable

`KComObject` adds three implementation refinements to this.

- **vtable sharing**: Upcall stubs and vtables are created once per method-signature sequence (shape) and shared by all instances. Event handlers are created in bulk, and per-instance stubs would consume native resources without bound. The object body is just 16 bytes — `{vtable pointer, instance key}` — and when a shared stub is called back it looks up the key in a global registry and delegates to that instance's Kotlin implementation (the lambda).
- **A prologue faithful to the convention**: The head of the vtable automatically gets IUnknown (QueryInterface / AddRef / Release) and, if needed, IInspectable (GetIids / GetRuntimeClassName / GetTrustLevel) implementations. The reference count is self-implemented with an `AtomicInteger` (1 at creation); while the count is alive the registry protects the lambda from GC, and the last `Release` from native code removes the registration, making it GC-eligible again. The invariant "protect the managed side while the count is alive" is the same one the .NET runtime imposes on CCWs.
- **Exception containment**: A Kotlin exception punching through an upcall into native code crashes the whole JVM. Dispatch therefore catches all exceptions, prints them to standard error, and converts them to an HRESULT (E_FAIL) to return. This containment is why an exception inside an event listener does not bring down the app — but by the same token, **exceptions inside listeners are swallowed and only appear on standard error**, so check the console when debugging.

### 16.5 Synthesizing the Application Subclass via COM Aggregation

A WinUI app demands a subclass of `Application`.
In C# you would write `class App : Application`, but a JVM class cannot directly inherit from a native class.
WinRT realizes this "inheritance across languages" through a mechanism called **COM aggregation**, and [WinUI4K](https://github.com/nttr-tech/winui4k) assembles it with FFI.

The idea is "represent inheritance as the fusion of two objects, an outer and an inner."

1. On the Kotlin side, create the outer. It is a COM object built with `KComObject` implementing `IApplicationOverrides` (the implementation of the virtual method `OnLaunched`) and `IXamlMetadataProvider` (XAML type resolution; the implementation forwards everything to the provider instance that ships with WinUI). This corresponds to "the parts overridden in the subclass."
2. Call `IApplicationFactory.CreateInstance(outer, &inner, &app)`. The native side creates the base class's implementation (inner), binds it to the outer, and returns the composed object (app).
3. From then on, from the XAML runtime's viewpoint, app is a single `Application` subclass. Virtual method calls are delegated to the outer (the Kotlin implementation), and QueryInterface for interfaces the outer does not know is delegated to the inner (the base implementation). This forwarding is handled by `KComObject`'s QI implementation.

The reason `OnLaunched` comes upcalled into the Kotlin side (Section 4.1) is this composition.
The two ingredients of inheritance — overriding and delegating to the base — are reproduced purely with COM's conventions.

### 16.6 SHA-1 Computation of Generic Instantiation IIDs

Instantiations of generic interfaces like `IVector<UIElement>` each have a unique IID per combination of type arguments.
Yet these IIDs are not recorded in winmd.
The WinRT specification defines them: assemble a **signature string** from the generic definition's IID and the type arguments, and run it through a name-based UUID (RFC 4122 version 5, SHA-1) to obtain the IID.

`Pinterface` (internal/winrt) implements this computation.
Using the WinRT-specific namespace GUID as the salt, it derives the UUID from the SHA-1 hash of the signature string (example: `pinterface({IID of the generic definition};rc(Microsoft.UI.Xaml.UIElement;{IID of the default interface}))`).
The instantiation IIDs of event `TypedEventHandler<T1, T2>`s are also obtained by this computation.

A computation error becomes the hard-to-chase bug "QI returns E_NOINTERFACE for no apparent reason," so the implementation is verified against a published known value (`IIterable<String>` = `e2fcc7c1-3bfc-5a0b-b2b0-72e769d1cb7e`).
You could call it the realization of the "never rely on guesswork" design principle (Section 1.2) in the territory where winmd has no values.

## Chapter 17: Mechanical Extraction of ABI Constants

### 17.1 winmd and tools/dump_winmd.py

COM calls require an IID per interface and a vtable slot number per method.
The primary source for these is winmd (Windows's type-information files, ECMA-335-format metadata).
WinUI's winmd is in the NuGet package (Microsoft.WindowsAppSDK.WinUI); the winmd for OS-side types (Windows.Foundation, etc.) is in the Windows SDK.

The bundled `tools/dump_winmd.py` is a tool that extracts the needed values from winmd.
Its only dependency is dnfile, a library for reading PE/.NET metadata.

```bash
pip install dnfile
python tools/dump_winmd.py Microsoft.UI.Xaml.winmd Microsoft.UI.Xaml.Controls.IButton
```

Depending on the kind of type, it outputs the following.

- **Interfaces**: The IID, and a slot listing in the form `vtbl[6+i]: MethodName(args) -> return`. WinRT interfaces have slots 0-2 fixed as IUnknown and 3-5 as IInspectable, so their own methods run from 6 in winmd declaration order.
- **Delegates**: The IID and `Invoke` (slot 3).
- **enums**: A listing of names and numeric values. The source of the enum constants in the `W*` API.
- **Structs**: Field order and types. Used for the FFI memory-layout definitions.
- **Classes**: The base, the default interface, and the factory kind (activatable / composable / statics). This is where it is decided which creation form of Section 16.3 is used.

### 17.2 The Zero-Guessed-Values Principle and How It Is Verified

The extracted values are placed as constants in the `*Interop` objects in `internal/winui`.
The files are split by winmd source (`XamlInterop` / `WindowingInterop` / `FoundationInterop` / `NotificationInterop` / `WebView2Interop`).

```kotlin
const val IID_IUIElement = "c3c01020-320c-5cf6-9d24-d396bbfa4d8b"
const val IUIElement_get_Opacity = 9
```

There is exactly one principle.
**Never hand-write IIDs or slot numbers from memory or guesswork. Always transcribe from dump_winmd.py's output.**

The reason is that the failure mode is the worst kind.
A wrong IID at least shows up understandably as E_NOINTERFACE, but a wrong slot number means "the neighboring method gets called."
If the signatures happen to be compatible, it does not error — it does something different, becoming a bug with unknown reproduction conditions.

Verification is two-tiered.
The correctness of the constants themselves can be checked mechanically by re-extracting from winmd and comparing (since the source is mechanical extraction, the comparison can be mechanical too).
On top of that, that the values actually function correctly is guaranteed by the E2E tests (Chapter 13) against real WinUI.
The handling of generic instantiation IIDs, which winmd lacks, is as described in Section 16.6.

## Chapter 18: The Lifetime-Management Implementation and Comparison with CsWinRT

Chapter 6 explained the automatic release machinery from the user's perspective; this chapter reads it as an implementation.
The design's foundation is CsWinRT, Microsoft's official interop runtime for C#; we contrast how the same problems were solved with the JVM's tools, and what could not be reproduced.

### 18.1 ComLifetime, the 3-State CAS, Premature Finalization and Fences

**ComLifetime.**
An ownership record created one per `W*` wrapper, corresponding to CsWinRT's `IObjectReference`.
It takes custody of the default interface obtained at creation via `adopt`, and appends each later-QI'd view (a pointer to another interface of the same object, each owning one unit of reference count) via `own`.
The point is to bundle all counts the wrapper owns in one place and release them in a single sweep.

**Separating out State.**
The information needed for release (the list of raw pointers and the status flag) is further separated from `ComLifetime` into a distinct object called `State`, and only that is handed to the GC registration.
If the cleanup action captured the wrapper itself, the registration would constitute a strong reference to the wrapper, which would then never become unreachable.
This is the easiest mistake to make in this kind of implementation, and it is a constraint arising from the API design of Java's `Cleaner`, not from CsWinRT.

**The 3-state CAS.**
There are two paths to release — via GC and via an explicit `close()` — and they can run concurrently.
`State` transitions NOT_DISPOSED → DISPOSE_PENDING → DISPOSE_COMPLETED with an `AtomicInteger` CAS, and only the side that wins the transition issues the release task, preventing double execution of `Release`.
This 3-state design is a port from CsWinRT.

**Premature finalization and fences.**
Tracing GCs have a counterintuitive property: even mid-method, the moment the JIT decides "this object's fields will not be read again," the object may be deemed unreachable.
There is a danger of premature finalization — cleanup running while a pointer has been read out and a native call is executing — and the countermeasure is guaranteeing liveness with `Reference.reachabilityFence` (on Java 8, the `synchronized (obj) {}` idiom).
However, the only place [WinUI4K](https://github.com/nttr-tech/winui4k) actually needs a fence is the completion guarantee of ownership registration (`ComLifetime.own`).
Because `Release` is funneled onto the UI thread's message loop (next item), a release running concurrently with a W* call executing on the UI thread is structurally impossible.

**ReleasePump.**
The posting slot that sends release tasks to the UI thread.
The body lives in the com layer, but the posting implementation (DispatcherQueue) lives in the winui layer, so to honor the layer dependency direction (winui → com), the winui layer injects the posting means at startup.
Tasks arriving before the UI thread is captured are held in reserve and flushed in bulk upon capture.

### 18.2 Switching between Cleaner / PhantomReference and Runtime Resolution via MethodHandle

The API for "run cleanup once unreachable" differs by Java version, so the underpinnings are switched at runtime (explicit selection via `-Dwinui4k.lifetime` is also possible).

| Environment | Cleanup | fence | FFI |
|---|---|---|---|
| Java 8 | `PhantomReference` + `ReferenceQueue` + homegrown daemon thread | `synchronized` idiom | JNA / JNR |
| Java 9-21 | `java.lang.ref.Cleaner` | `Reference.reachabilityFence` | JNA / JNR |
| Java 22+ | Same as above | Same as above | Panama |

The reason there are boundaries at two places, 9 and 22, is that the lifetime-management layer's boundary (Java 9's introduction of `Cleaner` / `reachabilityFence`) and the FFI layer's boundary (Java 22's finalization of FFM) are independent.

The homegrown implementation for Java 8 binds `PhantomReference`s to a `ReferenceQueue` with a daemon thread waiting on the queue — the same structure as Cleaner's own internals.
There is one trap: so that the PhantomReferences themselves are not GC'd, live registrations are strongly referenced in a set and removed once cleanup completes.

An interesting point is that the core module targets Java 8 (`-Xjdk-release=8`) yet manages to use Java 9's Cleaner.
This is achieved through runtime resolution via `Class.forName` and `MethodHandle`.
The FFI layer, under the same constraint, chose separate-module splitting (ServiceLoader) instead; the criterion for choosing between them is the breadth of the API surface.
An API where many types appear, like FFM, reads better with direct references in a separate module, while an API needing only a handful of methods, like Cleaner, is better served by runtime resolution, which adds nothing to the distribution.

One more JVM-specific design point is that the cleaner thread (`WinUI4K-Cleaner`) calls `RoInitialize(MTA)` at startup to join COM.
Cleanup only posts release tasks, but the posting itself (`DispatcherQueue.TryEnqueue`) is a COM call, and calls from a thread with an uninitialized apartment are to be avoided.
DispatcherQueue is agile (thread-indifferent), so it can be called safely from an MTA thread.

### 18.3 Correspondence Table with CsWinRT's Solutions

The mismatch between COM's reference counting and tracing GC breaks down into four parts (Chapter 6), and CsWinRT's and [WinUI4K](https://github.com/nttr-tech/winui4k)'s solutions correspond as follows.

| Mismatch | CsWinRT | [WinUI4K](https://github.com/nttr-tech/winui4k) |
|---|---|---|
| (1) No release notification | Two paths, finalizer and explicit `Dispose`, + 3-state CAS | Two paths, `Cleaner` / `PhantomReference` and `close()`, + 3-state CAS (ported) |
| (2) Native memory invisible to GC | `GC.AddMemoryPressure` (declaring a fixed amount per wrapper) | `NativeMemoryGovernor` (requests `System.gc()` at a reference-count threshold; opt-in) |
| (3) Cycles across the boundary | Automatic collection via `IReferenceTracker` integration with the .NET GC | **Unsolved**. The operational convention of removing listeners (Section 6.4) |
| (4) Release-thread constraint | Marshal to the creation-time context and `Release` | Funnel onto the UI thread via `ReleasePump` |

The difference at (4) comes from a difference in premises.
CsWinRT handles the general case of multiple apartments and so needs context recording and marshaling, whereas [WinUI4K](https://github.com/nttr-tech/winui4k) takes "one UI thread" as a premise (Chapter 5), so funneling all releases onto a single thread suffices.
An example of narrowing the premise to simplify the implementation.
The trade-off of not chasing the unreleased remainder at process exit (Section 4.3) is shared with CsWinRT.

### 18.4 What Cannot Be Reproduced on the JVM

Item (3) of the correspondence table is the only one that is fundamentally irreproducible on the JVM.

CsWinRT's cycle collection stands on an **extension point of the GC itself**: during the .NET GC's mark phase, the runtime and XAML's reference tracker query each other's object graphs.
The JVM's GC has no extension point for injecting an external reference graph into the mark phase, and it cannot be achieved in library code.
Sweeping the entire heap with a JVMTI agent is theoretically possible, but the cost of a stop-the-world sweep at every GC is impractical.

There is a gap at (2) as well.
The JVM has no general-purpose API corresponding to `GC.AddMemoryPressure` for declaring native allocation volume to the GC, and `NativeMemoryGovernor` remains an approximation that looks only at the count of references.

These two constraints are the grounds for the usage-side conventions stated in Chapter 6 (remove listeners explicitly; use `gcThreshold` if needed).
Put another way, [WinUI4K](https://github.com/nttr-tech/winui4k)'s lifetime management reproduces on the JVM "everything in CsWinRT's design except the parts that require modifying the language runtime."

## Chapter 19: Code Reading Guide

### 19.1 Getting Around the Repository

A lookup table from "what do you want to know" to the entry file.
Paths are relative to the core module, `winui4k/src/main/kotlin/com/appkitbox/winui4k/`.

| What you want to know | Entry point | Corresponding chapter |
|---|---|---|
| The whole of startup and shutdown | `WinUiUtilities.kt` (message loop, `Application` composition) | Chapter 4 |
| Posting to the UI thread | `internal/winui/Dispatcher.kt` | Chapter 5 |
| The plumbing of event subscription | `internal/winrt/Events.kt` and `Events.kt` (root) | Chapter 8 |
| Windows App SDK initialization | `internal/winui/Bootstrap.kt` | Section 2.4 |
| vtable calls | `internal/com/ComPtr.kt` | Section 16.1 |
| HRESULT-to-exception and diagnostics | `internal/com/Hresult.kt` | Section 14.1 |
| CCWs (upcalls) | `internal/winrt/KComObject.kt` | Section 16.4 |
| Object creation | `internal/winrt/Activation.kt` | Section 16.3 |
| Automatic reference release | The `internal/com/lifetime/` set | Chapter 18 |
| FFI abstraction and implementations | `internal/ffi/api/`, and the separate `winui4k-ffi-*` modules | Chapter 7 |
| ABI constants | `internal/winui/*Interop.kt` | Chapter 17 |
| E2E test infrastructure | `winui4k/src/test/.../UiTestHarness.kt` | Chapter 13 |

As for reading order, the efficient way is to pick one `W*` class and dive downward.
Starting from `WButton`, for example, you pass through all the layers in a single stroke: creation (`Activation`) → properties (`ComPtr` + the constants in `XamlInterop`) → events (`Events` + `KComObject`) → release (`ComLifetime`).
The `W*` classes themselves are a thin layer that merely "calls ComPtr using the Interop constants," so once you understand the layers below, every class starts to look the same.

### 19.2 Build Infrastructure

Common build settings are consolidated in convention plugins in `buildSrc`.

- **`winui4k.kotlin-common`**: The foundation for all modules. Builds always use the JDK 25 toolchain, with `-Xjdk-release` guaranteeing the target bytecode and API surface (Java 8 for the core, Java 22 for the panama module). The Spotless + ktlint (formatting) and detekt (static analysis) settings live here too.
- **`winui4k.kotlin-library`**: For published libraries. Holds the Maven Central publishing settings (POM, source JAR, signing).
- **`winui4k.kotlin-application`**: For sample apps. Adds `--enable-native-access` to the `run` task and performs the attribute adjustments needed to put the Java 22-targeted panama module on the runtime classpath of Java 8-targeted apps.
- **`winui4k.fat-jar`**: Fat JAR generation via the Shadow plugin. The merging of the FFI backends' `META-INF/services` (Section 15.1) is configured here.

The unusual part is how detekt is run.
Because detekt 1.23 does not run on JDK 25, instead of the Gradle plugin's in-process execution, detekt-cli is launched in a separate JDK 21 process (JDK 21 is fetched automatically by the foojay resolver).
Versions are managed centrally in `gradle/libs.versions.toml`, and including the multiple JDKs for testing, the only thing developers must prepare by hand is JDK 25 (Section 21.1).

# Part VI: Project Information

## Chapter 20: Versions and Compatibility

### 20.1 Supported-Environment Matrix

| Item | Supported range | Notes |
|---|---|---|
| OS | Windows 11 (expected to work on Windows 10 version 1809 or later as well) | A requirement of WinUI itself |
| Windows App SDK runtime | The 2.2 series | If absent, set up automatically at startup (Section 2.4) |
| Java (x64) | 8 or later | 8-21 use JNA / JNR; 22 and later use Panama (Chapter 7) |
| Java (ARM64) | 8 or later | 8-21: JNR only; 22 and later: Panama / JNR. The JDK itself must be an ARM64 build |
| Java (x86) | 8 or later | JNR only |

What CI verifies continuously is JDK 8 / 9 / 22 / 25 on x64 and JDK 25 on ARM64 (Section 13.3).
Other combinations are expected to work per the table above by design, but are verified less frequently.
For production adoption, it is recommended to exercise your target combinations directly in your own app's CI.

The version of the Windows App SDK depended on is embedded as a constant on the library side (currently the 2.2 series).
Since startup fails if the runtime's major version does not match (Section 2.5), when bumping the [WinUI4K](https://github.com/nttr-tech/winui4k) version, also align the bundled runtime installer (Section 15.2) to the corresponding release.

### 20.2 Versioning Policy and How to Read the Change History

The current version is in the 0.x series.
Per semantic-versioning convention, no public-API compatibility is guaranteed while in 0.x, and breaking changes may occur even between minor versions.
Understand this as the "prototype-stage library" positioning stated in Section 1.4 showing through in the version number.

Release artifacts come in two lines.

- **Maven Central**: The modules under the `com.appkitbox.winui4k` group. Normal dependency resolution goes here. Snapshot builds may be published to Central's snapshots repository.
- **GitHub Releases**: Each version tag (`v*.*.*`) has the all-in-one JAR (`winui4k-*-all.jar`) and the Gallery installer attached. Changes are recorded here too, so **the primary source for the change history is the GitHub Releases release notes**.

The practical upgrade checklist is: check the release notes for breaking changes → check for changes in the supported-environment matrix (including the depended-on Windows App SDK release) → run your own app's E2E tests (Chapter 13), in that order.

### 20.3 Roadmap

[WinUI4K](https://github.com/nttr-tech/winui4k) is a library NTT Resonant Technology prototyped, with one of its goals being application to the PC client of its own service (Section 1.4).
On the understanding that this should not be read as a committed roadmap, the directions written down in the repository are as follows.

- **Expanding the wrapped surface**: The current 60+ controls are a subset of WinUI, to be broadened according to demand. That the addition procedure is standardized (Section 21.3) is for this purpose.
- **Automatic binding generation**: Currently a person transcribes values extracted from winmd into `*Interop`, but this step is in a mechanizable form. A mechanism that auto-generates wrapper code from winmd, as an evolution of `tools/dump_winmd.py`, is positioned as the next step in growing this as a library.

Requests and bugs that affect the direction go to GitHub Issues (Section 21.4).
The material for evaluating the option of forking and maintaining in-house is provided by Part V.

## Chapter 21: Contribution

### 21.1 Development Environment Setup

All you need is JDK 25 (x64) and Windows 11.
Visual Studio, C++ build tools, and the .NET SDK are not used.

```powershell
git clone https://github.com/nttr-tech/winui4k.git
cd winui4k
.\gradlew run          # if Gallery launches, environment setup is complete
```

JDK 21 for detekt and JDK 8 / 9 / 22 for multi-version testing are fetched automatically by Gradle's foojay resolver as needed.
The commands frequently used during development are as follows.

```powershell
.\gradlew run                                  # launch Gallery (the basic means of verification)
.\gradlew build                                # build all modules + tests + detekt
.\gradlew :winui4k:test --tests "WButtonTest"  # run a single test class
.\gradlew :winui4k:testOnAllJavaVersions       # test on JDK 8 / 9 / 22 / 25
.\gradlew spotlessApply                        # format with ktlint
.\gradlew detekt                               # static analysis
.\gradlew :winui4k-sample-gallery:runJna       # Gallery with JDK 8 + JNA (verify Java 8 compatibility)
```

Tests are E2E, actually launching WinUI windows (Chapter 13).
They work over Remote Desktop, but not in headless environments.

### 21.2 Coding Conventions

Conventions are divided between two tools.

- **Formatting and naming: Spotless + ktlint**: Applied automatically by `spotlessApply`. There is no need to tidy by hand, and no style debates arise. Line endings across the repository are unified to LF.
- **All other static analysis: detekt**: Included in `build` and checked in CI too.

There is an ordering to how detekt findings are handled.

1. First consider fixing the code
2. If the code is intentional, `@Suppress` at the smallest scope with a reason attached (this project's standard technique; "to silence the tool" is not an acceptable reason)
3. Only when the rule itself does not fit the nature of the codebase, propose a change to the config file (`config/detekt/detekt.yml`)

No baseline file (bulk exemption of existing violations) is used.
The policy is that all exceptions are made visible in the code as `@Suppress`.

As one further convention, the first line of each `W*` class's KDoc names the corresponding Swing class (for `WButton`, "the JButton counterpart").

### 21.3 Procedure for Adding a New Control

Adding a control is standardized as a six-step procedure.
The underlying machinery is in Chapters 16 and 17.

1. **Obtain the winmd**: For WinUI types, use `Microsoft.UI.Xaml.winmd` from the NuGet package Microsoft.WindowsAppSDK.WinUI; for OS-side types, use the contract winmd from the Windows SDK.
2. **Extract the ABI values**: With `tools/dump_winmd.py`, dump IIDs and slots in the order: target class → default and base interfaces → related enums / delegates / structs (Section 17.1).
3. **Add constants to `*Interop.kt`**: Transcribe the extracted values into the file corresponding to the winmd source. Reuse existing constants for common interfaces like IControl.
4. **Implement the `W*` class**: Choose the base according to the parent (`WButtonBase` / `WControl` / `WContainer` / `WComponent`, etc.), and use the creation form matching the kind in winmd (`composeDefault` for composable, `activate` for activatable). Wrap long-held QI results with `own(...)` (Chapter 18).
5. **Add a demo page to Gallery**: Register it in the navigation and add an implementation example to the category page. The demo page doubles as the implementation example for users (Section 2.6).
6. **Verify**: Proceed through compile → visual check via `.\gradlew run` → adding E2E tests.

There are two iron rules to keep.
**IIDs / vtable slots / enum values must always be mechanically extracted from winmd — never written by hand, from memory, or by guess** (Section 17.2).
And in the core module, never import `java.lang.foreign` (preserving FFI backend independence; Section 3.2).

### 21.4 Issue / PR Etiquette

Bug reports, feature requests, and questions are all accepted via GitHub Issues.
No templates are provided, so including the following speeds up investigation.

- **Bug reports**: Reproduction steps (minimal code if possible), expected and actual results, console output (**always the full text of exception messages including the HRESULT**), and environment information (OS version, Java version and architecture, FFI backend, Windows App SDK runtime release). For display-related bugs, also attach a screenshot plus the display scaling setting and theme (light / dark).
- **Feature requests**: What you want to achieve, and the corresponding WinUI API name (if known).

The requirements for pull requests are as follows.

- `.\gradlew build` passes (including tests and detekt)
- Behavior additions and changes come with E2E tests (Section 13.2)
- Control additions follow the procedure and iron rules of Section 21.3

Corrections and improvement suggestions for the documentation, including this book, are likewise welcome via Issues.
The documentation is part of the repository (`doc/`), so you can also send a fix PR directly.
