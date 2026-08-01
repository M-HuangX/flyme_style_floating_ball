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

## 2. Lock Screen Auto-Hide (ABANDONED)

### Attempts

| Method | Result |
|--------|--------|
| `ACTION_USER_PRESENT` broadcast | HyperOS blocks this broadcast unless Autostart is enabled |
| `ACTION_SCREEN_ON` + `KeyguardManager.isKeyguardLocked()` | KeyguardManager returns incorrect values on HyperOS |
| Handler polling with `isDeviceLocked()` + `isInteractive()` | Unreliable timing, false positives |

### Conclusion

No reliable method found for HyperOS. Feature removed to save power. The ball remains visible on the lock screen.

---

## 3. `WRITE_SECURE_SETTINGS` Permission (REQUIRED)

### Issue

The app writes to `Settings.Secure.one_handed_mode_activated` to trigger one-handed mode. This requires `WRITE_SECURE_SETTINGS`, a signature-level permission that cannot be declared in the manifest for normal apps.

### Solution

Grant via ADB with "USB debugging (Security settings)" enabled in Developer Options:

```bash
adb shell pm grant com.floatingball android.permission.WRITE_SECURE_SETTINGS
```

On HyperOS, this requires the "USB debugging (Security settings)" toggle to be enabled (separate from regular USB debugging). Without this, the grant command fails with `SecurityException`.

---

## 4. Foreground Service on Android 14+ (SOLVED)

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

## 5. Android 8.0+ Overlay Type (SOLVED)

### Issue

`TYPE_PHONE` was deprecated in Android 8.0 (API 26). Using it triggers "displaying over other apps" notification.

### Solution

Use `TYPE_ACCESSIBILITY_OVERLAY` — this window type is designed for accessibility service overlays and does NOT trigger the overlay notification. It requires the accessibility service to be enabled.

---

## 6. Settings.Secure Key Not Persisted (SOLVED)

### Issue

On HyperOS, `one_handed_mode_activated` is briefly set to 1 during the enter transition, then reset to 0. The key oscillates: `0 → (our write) 1 → (HyperOS reset) 0 → (HyperOS writes back) 1`.

### Solution

Use `ContentObserver` (event-driven, no timing dependency) instead of polling. Observer fires on every write, including the final write-back at `STATE_ACTIVE`. No delay guessing required.

---

## 7. SharedPreferences Type Mismatch (SOLVED)

### Issue

`SeekBarPreference` stores values as `Integer`, but code used `getString()` to read them. Caused `ClassCastException: Integer cannot be cast to String`.

### Solution

Read with `getInt()` and catch `ClassCastException` as fallback for legacy data:

```kotlin
try { prefs.getInt(key, default) }
catch (_: ClassCastException) { prefs.getString(key, "$default")?.toIntOrNull() ?: default }
```

---

## 8. AAPT2 on ARM64 Linux (BUILD ENV)

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
