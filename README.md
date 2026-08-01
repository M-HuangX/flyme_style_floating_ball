# Floating Ball — Flyme-Style Navigation Ball

[中文](README_CN.md)

A floating navigation ball inspired by Meizu Flyme OS, for **Xiaomi HyperOS 3 (Android 16)**. Trigger Back, Home, Recents, Notifications, Quick Settings, and One-Handed Mode with simple gestures.

> ⚠️ **Tested only on HyperOS 3 (OS3.0.301.0.WPMEUXM, Android 16, myron).** One-handed mode exit detection depends on a HyperOS-specific `FLAG_TEST_ONLY` exemption. See [KNOWN_ISSUES.md](KNOWN_ISSUES.md).

## Features

| Gesture | Default Action |
|---------|---------------|
| Click | Back |
| Double-Click | Toggle One-Handed Mode |
| Swipe Up | Home (+ exit one-handed mode) |
| Swipe Down | Notifications |
| Swipe Left | Quick Settings |
| Swipe Right | Recent Apps |
| Long-Press + Drag | Move |

- One-handed mode with auto ball position compensation
- External exit detection (blank-area tap, timeout)
- Flyme-style inner circle swipe animation
- Auto-hide in landscape (video, camera)
- Fully customizable (size, opacity, ratio, all gesture actions)
- Persistent foreground service with health checks

## Requirements

- Xiaomi HyperOS 3
- ADB for initial setup

## Quick Start

```bash
# Install (the -t flag is REQUIRED for one-handed mode detection)
adb install -r -t app-debug.apk

# Grant permissions
adb shell pm grant com.floatingball android.permission.WRITE_SECURE_SETTINGS
adb shell dumpsys deviceidle whitelist +com.floatingball
```

Then: enable Accessibility Service → grant overlay permission → enable Autostart → set battery to "No restrictions".

## Known Issues

See [KNOWN_ISSUES.md](KNOWN_ISSUES.md) for detailed technical findings.

## Disclaimer

This project is for **educational and research purposes only**, studying Android accessibility services, window management, and system integration. It replicates Flyme OS visual design for academic purposes.

- **Not affiliated with Meizu or Xiaomi**
- **Commercial use prohibited**
- **Use at your own risk**

## License

MIT
