# BizFlow POS Windows Installer

## Requirements

- Windows 10 or newer, 64-bit
- JDK 21 with `java`, `javac`, and `jpackage` on `PATH`
- Apache Maven on `PATH`
- Inno Setup 6

## Build

Run:

```bat
build-installer.bat
```

The script:

1. Runs a clean Maven package.
2. Creates a self-contained application image with a Java 21 runtime.
3. Applies `src\main\resources\BizFlowPOS.ico` to the app and installer.
4. Compiles `BizFlowPOS.iss` using Inno Setup.

Always run `build-installer.bat` instead of compiling `BizFlowPOS.iss`
directly. The batch file regenerates `target\package-image` with the configured
Windows icon before Inno Setup packages it.

The final installer is:

```text
target\installer\BizFlowPOS-Setup-2.0.0.exe
```

If Inno Setup is installed in a custom directory, set:

```bat
set INNO_SETUP_COMPILER=C:\Path\To\ISCC.exe
build-installer.bat
```

The Inno `AppId` is a literal GUID in `BizFlowPOS.iss`. Keep it unchanged for
future releases so Windows upgrades the existing installation instead of
creating a second application entry.
