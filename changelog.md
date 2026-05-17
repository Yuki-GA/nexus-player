# Nexus Player v1.0.0-alpha

We are excited to announce the first public release of **Nexus Player**, a modern, console-style runtime frontend for Android. This release brings a complete UI/UX overhaul and major stabilization to the core runtime engine.

## Features & Improvements

* **Adaptive UI Redesign**: Fully adaptive dashboard optimized for both landscape (tablets, foldables, and landscape phones) and portrait modes, utilizing Jetpack Compose and Material 3 WindowSizeClass.
* **Control Center**: The Settings screen has been expanded into a fully customizable Control Center, featuring modular categories for Appearance, Layout, Runtime, Input, Library, and System maintenance.
* **Futuristic Aesthetic**: Glassmorphism surfaces, neon blue accents, and smooth spring-based animations bring a professional, console-like feel to the environment.
* **Runtime Stabilization Barrier**: Fixed critical race conditions during Android Webview orientation and layout transitions. The rendering loop now suspends during invalid `0x0` dimension states and recovers cleanly, eliminating vertical tilemap artifacts and preventing engine corruption.
* **Robust Input Bridge**: Virtual controls and gamepad routing have been hardened for immediate responsiveness and strict lifecycle persistence, seamlessly binding Android inputs to RPG Maker's internal state.
* **Diagnostic Telemetry**: A live virtual terminal and real-time telemetry widgets (CPU, GPU, VFS, FPS) allow for deep performance observation without cluttering the gameplay view.
* **VFS Mount System**: The Virtual File System (VFS) accurately mounts and proxies game assets securely, avoiding scoped storage bottlenecks and reducing load times.

## Fixes

* Resolved sub-pixel and fractional CSS scaling issues causing tilemap grid seams.
* Corrected Webview focus loss and input stalls after scene transitions.
* Fixed persistent blank screens occurring when returning from the library.
* Eliminated overzealous renderer polling that caused UI layout breakage.

Welcome to the new era of Nexus Player!
