# Known Issues & Technical Deep Dive

## 1. One-Handed Mode External Exit Detection (SOLVED)

### The Problem

Android provides no public API to query or observe one-handed mode state. Third-party apps cannot read the system's internal state. When the user exits one-handed mode via external means (tapping blank area, system timeout, task switch), our app has no way to detect it.

### Investigation

We attempted 7 different approaches before finding a solution:

| # | Method | Result | Root Cause |
|---|--------|--------|------------|
| 1 | `Settings.Secure.getInt("one_handed_mode_activated")` | Always returns 0 | Android 12+ blocks @hide keys from third-party reads |
| 2 | `ContentResolver.query()` raw query | Explicit error | ContentProvider-level restriction: "@hide keys restricted to system_server and system apps" |
| 3 | `Runtime.exec("settings get secure ...")` | SecurityException | Requires `INTERACT_ACROSS_USERS` (signature permission) |
| 4 | ContentObserver | Never fired (implementation bug) | Observer was defined but never instantiated in `FloatingBallService` |
| 5 | Accessibility `TYPE_ANNOUNCEMENT` | No relevant events | HyperOS does not announce one-handed mode changes via accessibility |
| 6 | Display/Window metrics | No difference | HyperOS uses hardware-level (SurfaceFlinger) display transform |
| 7 | `dumpsys` / `getprop` | No change | No shell-detectable state difference |

### Solution

ROM analysis of HyperOS 3 (OS3.0.301.0.WPMEUXM) revealed a `FLAG_TEST_ONLY` exemption in both `Settings.NameValueCache` and `SettingsProvider`:

- **`Settings.NameValueCache.isCallerExemptFromReadableRestriction()`** checks `ApplicationInfo.flags & 256` (FLAG_TEST_ONLY)
- **`SettingsProvider.enforceSettingReadable()`** skips @Readable check when `FLAG_TEST_ONLY` is set
- **`ContentService.registerContentObserver()`** does NOT validate per-key readability — observer registration and notification delivery work regardless

Three changes required:

1. `android:testOnly="true"` in AndroidManifest.xml
2. `adb install -r -t app-debug.apk` (the `-t` flag is mandatory)
3. ContentObserver properly registered in `FloatingBallService.onServiceConnected()`

```kotlin
oneHandedObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
    override fun onChange(selfChange: Boolean) {
        if (selfChange) return // ignore our own writes
        val nowActive = Settings.Secure.getInt(contentResolver,
            "one_handed_mode_activated", 0) == 1
        if (!nowActive && oneHandedCompensated) {
            OneHandedModeHelper.forceReset()
            animateCompensation(false) // restore ball position
        }
    }
}
contentResolver.registerContentObserver(
    Settings.Secure.getUriFor("one_handed_mode_activated"), false, observer)
```

### Why `testOnly=true`?

When an app is flagged as test-only, the HyperOS framework treats it similarly to a system app for Settings key readability. This exemption is NOT part of standard AOSP — it exists in HyperOS's customized `SettingsProvider`. If Xiaomi removes this exemption in a future ROM update, external exit detection will break.

---

## 2. Lock Screen Auto-Hide (SOLVED — v1.9)

### The Problem

Initially thought HyperOS blocked lock screen detection. All early attempts appeared to fail.

### The Real Issue

Three misconceptions were debunked by diagnostic instrumentation on the target device:

1. `ACTION_USER_PRESENT` was missed because the `BroadcastReceiver` was not registered with `RECEIVER_EXPORTED`. With the correct flag, it fires reliably.
2. `KeyguardManager.isKeyguardLocked()` works correctly — we were using `isDeviceLocked()` which has different semantics (false when Swipe/Smart Lock is active).
3. The ball's state management conflated landscape and lockscreen hide reasons, causing one to override the other.

### Solution

`LockscreenController` (pure event-driven):
- **DisplayListener** — screen ON/OFF/DOZE state changes
- **SCREEN_OFF broadcast** — immediate hide
- **SCREEN_ON/USER_PRESENT** — trigger reconcile → `isKeyguardLocked()`
- **Zero polling, zero battery impact**

Key insight: `isKeyguardLocked()` returns correct values on HyperOS 3. There is no "OEM interference" — we were just using the wrong API and wrong receiver flags.

## 3. Keyboard Avoidance (SOLVED — v1.11)

### The Problem

Ball overlaps the keyboard when typing. Initial approach using `AccessibilityService.windows` (TYPE_INPUT_METHOD) worked in some apps (WeChat, browser) but not others (system search, Xiaohongshu).

### The Real Issue

Accessibility window enumeration (`getWindows()`) is not a reliable keyboard detection API. Some app contexts don't expose the IME window to accessibility. Additionally, the ball was positioned ABOVE the IME due to missing `FLAG_ALT_FOCUSABLE_IM`.

### Solution

Standard Android `WindowInsets` API:
- **`FLAG_ALT_FOCUSABLE_IM`** on the ball window — allows receiving IME insets while remaining non-focusable (official Android pattern)
- **`OnApplyWindowInsetsListener`** — triggers 48ms debounced capture
- **`WindowMetrics.windowInsets`** — reads full-screen IME state (not clipped by small ball window)
- **API 35 `getBoundingRects(Type.ime())`** — precise occlusion detection for floating/split keyboards
- **`isAvoidingKeyboard` state machine** — prevents oscillation during keyboard show/hide animation

This works in ALL scenarios because WindowInsets is a WMS-level mechanism independent of accessibility event delivery.

## 4. Service Persistence / Health Check (SOLVED — v1.12)

### The Problem

Used to have a 15-second `AlarmManager` health check as the only recurring CPU wakeup in the app.

### The Real Issue

The health check was redundant. It never restarted the service — only checked if accessibility was still enabled.

### Solution

ROM analysis (SERVICE_PERSISTENCE_SOLUTION.md) confirmed HyperOS 3 already has `BIND_AUTO_CREATE + Binder death + ActiveServices` auto-recovery. The AlarmManager was removed. All other protections retained (foreground service, autostart, battery no-restrictions, deviceidle whitelist, `onUnbind() returns true`).

**Result: The entire app is now zero-polling — all components are event-driven.**


---

## 5. `WRITE_SECURE_SETTINGS` Permission (REQUIRED)

### Issue

The app writes to `Settings.Secure.one_handed_mode_activated` to trigger one-handed mode. This requires `WRITE_SECURE_SETTINGS`, a signature-level permission that cannot be declared in the manifest for normal apps.

### Solution

Grant via ADB with "USB debugging (Security settings)" enabled in Developer Options:

```bash
adb shell pm grant com.floatingball android.permission.WRITE_SECURE_SETTINGS
```

On HyperOS, this requires the "USB debugging (Security settings)" toggle to be enabled (separate from regular USB debugging). Without this, the grant command fails with `SecurityException`.

---

## 6. Foreground Service on Android 14+ (SOLVED)

### Issue

Android 14+ requires `foregroundServiceType` declaration. Accessibility services that show overlays must use `specialUse` type with a justification.

### Solution

```xml
<service
    android:foregroundServiceType="specialUse"
    ...>
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="This accessibility service maintains a floating navigation ball overlay..." />
</service>
```

---

## 7. Android 8.0+ Overlay Type (SOLVED)

### Issue

`TYPE_PHONE` was deprecated in Android 8.0 (API 26). Using it triggers "displaying over other apps" notification.

### Solution

Use `TYPE_ACCESSIBILITY_OVERLAY` — this window type is designed for accessibility service overlays and does NOT trigger the overlay notification. It requires the accessibility service to be enabled.

---

## 8. Settings.Secure Key Not Persisted (SOLVED)

### Issue

On HyperOS, `one_handed_mode_activated` is briefly set to 1 during the enter transition, then reset to 0. The key oscillates: `0 → (our write) 1 → (HyperOS reset) 0 → (HyperOS writes back) 1`.

### Solution

Use `ContentObserver` (event-driven, no timing dependency) instead of polling. Observer fires on every write, including the final write-back at `STATE_ACTIVE`. No delay guessing required.

---

## 9. SharedPreferences Type Mismatch (SOLVED)

### Issue

`SeekBarPreference` stores values as `Integer`, but code used `getString()` to read them. Caused `ClassCastException: Integer cannot be cast to String`.

### Solution

Read with `getInt()` and catch `ClassCastException` as fallback for legacy data:

```kotlin
try { prefs.getInt(key, default) }
catch (_: ClassCastException) { prefs.getString(key, "$default")?.toIntOrNull() ?: default }
```

---

## 10. AAPT2 on ARM64 Linux (BUILD ENV)

### Issue

Android build-tools only provide x86_64 binaries for Linux. AAPT2 fails on ARM64 servers.

### Solution

```bash
sudo apt-get install -y qemu-user-static libc6-amd64-cross
export QEMU_LD_PREFIX=/usr/x86_64-linux-gnu
```

Add to `gradle.properties`:
```
systemProp.QEMU_LD_PREFIX=/usr/x86_64-linux-gnu
```
