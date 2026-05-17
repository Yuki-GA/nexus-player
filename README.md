# Nexus Player

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Version](https://img.shields.io/badge/version-1.0.0--alpha-neonblue)

**Nexus Player** is a modern, console-style runtime frontend designed specifically for RPG Maker MV and MZ games on Android. It transforms the mobile gaming experience into a high-end, immersive platform with a futuristic aesthetic and robust engine stability.

## 🚀 Key Features

*   **Adaptive Dashboard**: A fully responsive UI optimized for landscape (tablets, foldables, phones) and portrait modes, built with Jetpack Compose and Material 3.
*   **Persistent Runtime Architecture**: The game engine is decoupled from the UI, allowing it to survive navigation, orientation changes, and backgrounding without reloading or losing state.
*   **Lifecycle Stabilization Barrier**: Surgical fixes for the common "0x0 dimension" race condition in Android WebViews, ensuring stable rendering and eliminating vertical tilemap artifacts.
*   **Authoritative Input Bridge**: Emulates a professional PC keyboard environment with zero-latency synchronization directly into the RPG Maker engine loop.
*   **Control Center**: A centralized hub for deep customization, including UI styling (Glassmorphism, AMOLED mode), performance targets (FPS limit, HW acceleration), and input remapping.
*   **Virtual File System (VFS)**: High-speed proxied asset loading that bypasses traditional Android scoped storage bottlenecks while maintaining security.
*   **Real-time Telemetry**: Integrated terminal and monitoring widgets for CPU, GPU, memory, and engine lifecycle events.

## 🎨 Visual Direction

Nexus Player features a **Futuristic Dark UI** inspired by modern gaming consoles:
*   **Glassmorphism surfaces** with subtle translucency and soft glows.
*   **Neon Blue accents** across the entire interface.
*   **Spring-based animations** for a fluid, premium feel.
*   **Console-style navigation** with a collapsible sidebar and context-sensitive main stages.

## 🛠️ Technical Overview

*   **Architecture**: MVVM with a persistent Activity-scoped ViewModel.
*   **Frontend**: Jetpack Compose with Material 3 Adaptive Layouts.
*   **Backend**: Optimized Android WebView (Chromium) with custom JavaScript-to-Kotlin bridge logic.
*   **Native Interop**: Authoritative PC-style key event dispatching and direct RPG Maker internal state mutation.

## 📂 Getting Started

1.  Download the latest [Release](https://github.com/Yuki-GA/nexus-player/releases).
2.  Install the APK on your Android device (min SDK 24).
3.  Import your RPG Maker MV/MZ game folder.
4.  Launch and enjoy a console-quality experience!

## 🧪 Alpha Status

This is an early-stage release. We are currently in the **Alpha** phase, focusing on engine stability and UI architecture. Debug builds are available for users who wish to provide forensic logs for device-specific optimization.

---
Developed with ❤️ by the Nexus Team.
