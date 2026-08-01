# Floating Ball — Flyme-Style Navigation Ball

[中文](README_CN.md)

> # ⚠️ DISCLAIMER — PLEASE READ ⚠️
>
> **This project is for EDUCATIONAL AND RESEARCH PURPOSES ONLY.**
>
> It replicates the visual design and interaction patterns of Meizu's Flyme OS floating ball **solely to study** Android accessibility services, window management, and system integration techniques.
>
> - **NOT affiliated with Meizu Technology Co., Ltd. or Xiaomi Inc.**
> - **COMMERCIAL USE IS STRICTLY PROHIBITED.**
> - **USE AT YOUR OWN RISK.** The authors provide NO WARRANTY of any kind, express or implied. The authors are NOT responsible for any damage, data loss, system instability, or legal consequences resulting from the use of this software.
> - This project contains **NO proprietary code** from Meizu, Xiaomi, or any third party. All code is original work created for academic study.
>
> **By using this software, you acknowledge that you have read and understood this disclaimer.**

---

A floating navigation ball for **Xiaomi HyperOS 3 (Android 16)**. Trigger Back, Home, Recents, Notifications, Quick Settings, and One-Handed Mode with simple gestures.

> ⚠️ **Tested only on HyperOS 3 (OS3.0.301.0.WPMEUXM, Android 16, myron).** See [KNOWN_ISSUES.md](KNOWN_ISSUES.md).

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

- One-handed mode with auto ball position compensation + external exit detection
- Flyme-style inner circle swipe animation
- Auto-hide in landscape (video, camera)
- Fully customizable (size, opacity, ratio, gesture actions)
- Persistent foreground service with 15s health checks

## Requirements

- Xiaomi HyperOS 3
- ADB for initial setup

## Quick Start

```bash
adb install -r -t app-debug.apk
adb shell pm grant com.floatingball android.permission.WRITE_SECURE_SETTINGS
adb shell dumpsys deviceidle whitelist +com.floatingball
adb shell cmd appops set com.floatingball RUN_IN_BACKGROUND allow
adb shell cmd appops set com.floatingball RUN_ANY_IN_BACKGROUND allow
```

Then: enable Accessibility Service → grant overlay permission → enable Autostart → set battery to "No restrictions".

## Known Issues

See [KNOWN_ISSUES.md](KNOWN_ISSUES.md).

## License

MIT
