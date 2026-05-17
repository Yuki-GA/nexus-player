# Nexus Player v1.1.0-alpha

This release marks a major milestone in the evolution of Nexus Player, transitioning from a conceptual UI to a professional, data-driven gaming runtime platform.

## 🌟 Real-World Data & Telemetry Overhaul

*   **Live Telemetry Dashboard**: The telemetry panel is now bound to real runtime flows. It provides accurate, real-time data for Engine Type (NATIVE_SDL2 vs WEBVIEW), Memory Usage (JVM Metrics), and VFS Mount Status.
*   **Active Terminal Console**: Replaced placeholder logs with a live `telemetryLogs` feed, capturing every internal state transition and engine initialization event.
*   **Native FPS Reporting**: Injected a high-performance FPS counter directly into the game bootstrapper, reporting actual engine frames-per-second to the dashboard.

## 🛠️ Advanced Customization & Control Center

*   **Persistent User Profiles**: All settings are now backed by Jetpack DataStore. UI preferences like Glassmorphism FX, AMOLED mode, and Corner Radius persist across app restarts.
*   **Live UI Previews**: Changes made in the Control Center propagate instantly through the interface without requiring a restart.
*   **Category Reset**: Added "Reset to Default" functionality for Appearance, Input, and Runtime categories.

## 🎮 Virtual Controller v2.0

*   **Persistent Layout Editor**: Draggable control positions are now saved to the device. Customize your D-pad and Action cluster once, and it stays where you want it.
*   **Real Haptic Feedback**: Integrated the Android Vibrator system for low-latency tactile feedback on every button press.
*   **Pro Ergonomics**: Improved D-pad diagonal detection and high-fidelity Glassmorphism visuals with active-press scale animations.

## 🔧 Core Stabilization

*   **Lifecycle Barrier v14.0**: Surgical fixes for WebView 0x0 dimension race conditions. The update loop now suspends during layout transitions, preventing vertical tilemap artifacts and framebuffer corruption.
*   **Focus Management**: Hardened focus recovery ensures the engine correctly regains input priority after resizing or returning from the dashboard.

## 📦 Build Artifacts
This release includes both the **Performance Release** and the **Forensic Debug** binaries.
