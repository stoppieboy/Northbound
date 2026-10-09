# Northbound

A polished Android compass built with Jetpack Compose. The dial follows the device's rotation-vector sensor and displays a magnetic heading in degrees and cardinal directions. It needs no permissions or network access.

## Build

Open this folder in Android Studio with Android SDK 35 installed, then run the `app` configuration on a device or emulator with a rotation-vector sensor. The project uses JDK 17 for Gradle builds.

Alternatively, from this folder run `gradle :app:assembleDebug` if Gradle is installed.

The generated APK will be at `app/build/outputs/apk/debug/app-debug.apk`.
